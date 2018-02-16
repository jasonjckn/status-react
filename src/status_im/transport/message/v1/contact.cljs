(ns status-im.transport.message.v1.contact
  (:require [status-im.transport.message.core :as message]))

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

;; TODO (yenda) decide how to work with those. ideally the payload should be a vector of records that
;; implement receive


{:chat-id-1 {:received [:id1 :id2 :id3]
             :seen [:id1 :id2]
             :pending [:message1 :message2]
             :sim-key-id :key-id
             :topic contact-topic}}


(defrecord NewKey [password])

(defrecord Received [message-ids]
  {:db [add-timeout-to-send-message]})

(defrecord Seen [message-ids]
  {:db [add-timeout-to-send-message]})



;; TODO (yenda) decide what send should actually do
(defrecord ContactRequest [name profile-image address fcm-token]
  message/StatusMessage
  (send [this public-key]
    {:db (-> db
             (assoc-in [:transport chat-id :pending] {:sig identity
                                                      :ttl 2000
                                                      :payload [this]
                                                      :topic contact-topic}))
     :dispatch [:whisper/send-messages chat-id]})
  (receive [this signature]
    (message/receive-contact-request cofx public-key payload)))

(defrecord ContactRequestConfirmed [name profile-image address fcm-token]
  message/StatusMessage
  (send [this public-key]
    {:sig identity
     :pubKey chat-id
     :ttl 2000
     :payload [this]
     :topic contact-topic})
  (receive [this signature]
    (message/receive-contact-request-confirmation cofx signature)))

(defrecord ContactMessage [content]
  message/StatusMessage
  (send [this public-key]
    {:sig identity
     :pubKey chat-id
     :ttl 2000
     :payload [this]
     :topic contact-topic})
  (receive [this signature]
    {:dispatch [:pre-received-message (assoc payload
                                             :chat-id chat-id
                                             :from    signature)]}))
