(ns status-im.transport.message.v1.contact
  (:require [status-im.transport.message.core :as message]
            [status-im.transport.message.v1.protocol :as protocol]))

(defrecord ContactRequest [name profile-image address fcm-token]
  message/StatusMessage
  (send [message cofx chat-id]
    (-> cofx
        (protocol/requires-ack chat-id)
        (protocol/generate-new-key-and-password chat-id)))
  (receive [message cofx chat-id signature]
    (-> cofx
        (protocol/ack chat-id message-id)
        (message/receive-contact-request public-key payload))))

(defrecord ContactRequestConfirmed [name profile-image address fcm-token]
  message/StatusMessage
  (send [message cofx chat-id]
    (-> cofx
        (protocol/requires-ack chat-id)
        (protocol/send chat-id message))
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
