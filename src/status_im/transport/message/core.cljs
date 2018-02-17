(ns status-im.transport.message.core)

(def ping-topic "0x01010202")

(def ttl 10000)

(defprotcol StatusMessage
  (send [message cofx chat-id])
  (receive [message cofx chat-id signature]))

;;TODO (yenda) this is probably not the place to have these
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
