(ns status-im.transport.message.v1.contact
  (:require [status-im.transport.message.core :as message]
            [status-im.transport.message.v1.protocol :as protocol]))

(defrecord ContactRequest [name profile-image address fcm-token]
  message/StatusMessage
  (send [this cofx chat-id]
    (-> cofx
        (protocol/generate-new-key-and-password chat-id)
        (protocol/send this))
    #_(-> cofx
          (protocol/requires-ack chat-id)
          (protocol/generate-new-key-and-password chat-id)))
  (receive [this cofx chat-id signature]
    (message/receive-contact-request cofx signature this)
    #_(-> cofx
          (protocol/ack chat-id message-id)
          (message/receive-contact-request signature payload))))

(defrecord ContactRequestConfirmed [name profile-image address fcm-token]
  message/StatusMessage
  (send [this cofx chat-id]
    (protocol/send cofx this)
    #_(-> cofx
          (protocol/requires-ack chat-id)
          (protocol/send chat-id this)))
  (receive [this cofx chat-id signature]
    #_(protocol/receive db #{:ack})
    (message/receive-contact-request-confirmation cofx signature this)))

(defrecord ContactMessage [content]
  message/StatusMessage
  (send [this cofx chat-id]
    (protocol/send cofx this))
  (receive [this cofx chat-id signature]
    {:dispatch [:pre-received-message (assoc content
                                             :chat-id chat-id
                                             :from    signature)]}))
