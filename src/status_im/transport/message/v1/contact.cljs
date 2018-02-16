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
(defrecord NewKey [password])
(defrecord Ack [message-ids])


;; TODO (yenda) decide what send should actually do
(defrecord ContactRequest [name profile-image address fcm-token]
  message/StatusMessage
  (send [this public-key]
    {:sig identity
     :pubKey chat-id
     :ttl 2000
     :payload [this]
     :topic contact-topic})
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
