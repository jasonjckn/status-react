(ns status-im.transport.handlers
  (:require [re-frame.core :as re-frame]
            [status-im.utils.handlers :as handlers]
            [status-im.transport.message-cache :as message-cache]
            [status-im.transport.message :as message]
            [status-im.chat.models :as models.chat]
            [status-im.utils.datetime :as datetime]
            [taoensso.timbre :as log]
            [status-im.transport.utils :as web3.utils]
            [cljs.reader :as reader]))


(re-frame/reg-fx
  :stop-whisper
  (fn [] (protocol/stop-whisper!)))

(re-frame/reg-fx
  ::init-whisper
  (fn [{:keys [web3 public-key groups updates-public-key updates-private-key status contacts pending-messages]}]
    (protocol/init-whisper!
     {:web3                        web3
      :identity                    public-key
      :groups                      groups
      :callback                    #(re-frame/dispatch [:incoming-message %1 %2])
      :ack-not-received-s-interval 125
      :default-ttl                 120
      :send-online-s-interval      180
      :ttl-config                  {:public-group-message 2400}
      :max-attempts-number         3
      :delivery-loop-ms-interval   500
      :profile-keypair             {:public  updates-public-key
                                    :private updates-private-key}
      :hashtags                    (mapv name (handlers/get-hashtags status))
      :pending-messages            pending-messages
      :contacts                    (keep (fn [{:keys [whisper-identity
                                                      public-key
                                                      private-key]}]
                                           (when (and public-key private-key)
                                             {:identity whisper-identity
                                              :keypair  {:public  public-key
                                                         :private private-key}}))
                                         contacts)
      :post-error-callback         #(re-frame/dispatch [::post-error %])})))

(defn get-message-id [status-message]
  (web3.utils/sha3 status-message))

(defn serialize-status-message [status-message]
  (prn-str status-message))

(defn deserialize-status-message [status-message]
  (reader/read-string status-message))

(defn parse-payload [message]
  (let [{:keys [payload sig recipientPublicKey]}            (js->clj js-message :keywordize-keys true)
        {:keys [protocol transport-message status-message]} (-> payload
                                                                web3.utils/to-utf8
                                                                reader/read-string)
        {:keys [message-type acks]}                         transport-message]
    {:signature         sig
     :recipient         recipientPublicKey
     :protocol          protocol
     :message-type      message-type
     :message-id        (get-message-id status-message)
     :acks              acks
     :status-message    (deserialize-status-message status-message)}))

(defn deduplication [{:keys [status-message] :as message}]
  (if status-message
    (let [message-id (get-message-id status-message)]
      (when-not (message-cache/exists? message-id)
        (message-cache/add! message-id)
        (-> message
            (assoc :message-id message-id)
            (update :status-message deserialize-status-message))))
    message))

(handlers/register-handler-fx
  :protocol/receive-whisper-message
  [re-frame/trim-v]
  (fn [{:keys [db]} [js-error js-message]]
    (let [{:keys [signature recipient message-type status-message]} (some-> js-message
                                                                            parse-payload
                                                                            deduplication)]
      (when status-message
        {:dispatch [:protocol/receive-status-message signature recipient message-type status-message]}))))

(defn- receive-contact-request
  [{{:contacts/keys [contacts] :as db} :db :as cofx}
   public-key
   {:keys [name profile-image address fcm-token]}]
  (when-not (get contacts public-key)
    (let [contact-props {:whisper-identity public-key
                         :public-key       public-key
                         :address          address
                         :photo-path       profile-image
                         :name             name
                         :fcm-token        fcm-token
                         :pending?         true}
          fx            {:db           (update-in db [:contacts/contacts public-key] merge contact-props)
                         :save-contact contact-props}
          chat-props    {:name         name
                         :chat-id      public-key
                         :contact-info (prn-str contact-props)}]
      (merge fx (models.chat/add-chat (assoc cofx :db (:db fx)) public-key chat-props)))))

(defn- receive-contact-request-confirmation
  [{{:contacts/keys [contacts] :as db} :db :as cofx} public-key {:keys [name profile-image address fcm-token]}]
  (when-let [contact (get contacts public-key)]
    (let [contact-props {:whisper-identity public-key
                         :address          address
                         :photo-path       profile-image
                         :name             name
                         :fcm-token        fcm-token}
          fx            {:db           (update-in db [:contacts/contacts public-key] merge contact-props)
                         :save-contact contact-props}
          chat-props    {:name    name
                         :chat-id public-key}]
      (merge fx (models.chat/upsert-chat (assoc cofx :db (:db fx)) chat-props)))))

(def message-type->handler
  {:contact/request (fn [cofx public-key _ payload]
                      (receive-contact-request cofx public-key payload))
   :contact/request-confirmed (fn [cofx public-key _ payload]
                                (receive-contact-request-confirmation cofx public-key payload))
   :contact/message (fn [_ public-key chat-id payload]
                      {:dispatch [:pre-received-message (assoc payload
                                                               :chat-id chat-id
                                                               :from    public-key)]})})

(handlers/register-handler-fx
  :protocol/receive-status-message
  [re-frame/trim-v]
  (fn [cofx [signature chat-id message-type status-message]]
    (if-let [handler (get message-type->handler message-type)]
      (handler cofx signature chat-id status-message)
      (log/error :unknown-message-type message-type))))


(handlers/register-handler-fx
  :protocol/send-status-message
  [re-frame/trim-v]
  (fn [{:keys [db]} [chat-id message-type status-message]]
    {:shh/post (merge {:web3          (:web3 db)
                       :success-event :protocol/send-status-message-success
                       :error-event   :protocol/send-status-message-error}
                      (message/send-options {:message-type   message-type
                                             :db             db
                                             :chat-id        chat-id
                                             :status-message (prn-str status-message)}))}))

(handlers/register-handler-fx
  :protocol/send-status-message-success
  [re-frame/trim-v]
  (fn [{:keys [db] :as cofx} [_ resp]]
    (log/debug :send-status-message-success resp)))

(handlers/register-handler-fx
  :protocol/send-status-message-error
  [re-frame/trim-v]
  (fn [{:keys [db] :as cofx} [err]]
    (log/error :send-status-message-error err)))
