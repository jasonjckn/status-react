(ns status-im.transport.message)

(def ping-topic "0x01010202")

(def ttl 10000)

(def protocol-version 0)

(defn message-type-options [message-type]
  (get message-type
       {:contact/request {:require-ack? true}
        :contact/message {:require-ack? true}}
       {}))

(defmulti send-options :message-type)

(defmethod send-options :contact/request
  [{:keys [message-type db chat-id status-message]}]
  {:message {:sig (:current-public-key db)
             :pubKey chat-id
             :ttl ttl
             :payload [0 :contact/request status-message]
             :topic ping-topic #_web3.filtering/status-topic}})

(defmethod send-options :contact/request-confirmed
  [{:keys [message-type db chat-id status-message]}]
  {:message {:sig (:current-public-key db)
             :pubKey chat-id
             :ttl ttl
             :payload [0 :contact/request status-message]
             :topic ping-topic #_web3.filtering/status-topic}})

(defmethod send-options :contact/message
  [{:keys [message-type db chat-id status-message]}]
  {:message {:sig (:current-public-key db)
             :pubKey chat-id
             :ttl ttl
             :payload [0 :contact/message status-message]
             :topic ping-topic #_web3.filtering/status-topic}})

;; :message
;; :group-message
;; :public-group-message
;; :pending
;; :sent
;; :ack
;; :seen
;; :group-invitation
;; :update-group
;; :add-group-identity
;; :remove-group-identity
;; :leave-group
;; :contact-request
;; :discover
;; :discoveries-request
;; :discoveries-response
;; :profile
;; :update-keys
;; :online
