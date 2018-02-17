(ns status-im.transport.message.v1.contact
  (:require [status-im.transport.message.core :as message]
            [status-im.transport.message.v1.protocol :as protocol]))

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

;; TODO could we prepare everything and make
;; generate-sym-key-and-password callback the shh/post function directly
;; might be hard because how do we add sym-key-id to db ?
;; callback is already a re-frame/dispatch so might be ok to just save sym-key-id in the dispatch
;; and send message directly in callback
(handler/register-handler-fx
  ::send-contact-request
  (fn [cofx [_ password sym-key-id]]
    (protocol/send db message #{:ack :new-key})))

(defrecord ContactRequest [name profile-image address fcm-token]
  message/StatusMessage
  (send [message db chat-id]
    {:shh/generate-sym-key-and-password {:success-event :send-contact-request}})
  (receive [message db chat-id signature]
    (protocol/receive db message #{:ack})
    (message/receive-contact-request cofx public-key payload)))

(defrecord ContactRequestConfirmed [name profile-image address fcm-token]
  message/StatusMessage
  (send [this public-key]
    (protocol/send db message #{:ack}))
  (receive [this signature]
    (protocol/receive db message #{:ack})
    (message/receive-contact-request-confirmation cofx signature)))

(defrecord ContactMessage [content]
  message/StatusMessage
  (send [message db chat-id]
    (protocol/send db message #{:ack}))
  (receive [message db chat-id signature]
    {:dispatch [:pre-received-message (assoc payload
                                             :chat-id chat-id
                                             :from    signature)]}))
