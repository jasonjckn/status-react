(ns status-im.transport.message.v1.protocol)

(def ttl (* 3600 1000)) ;; ttl of one hour

(defrecord NewKey [password]
  (receive [message db chat-id]
    {:dispatch [:generate-sym-key-from-password password]}))

(defrecord Ack [message-ids]
  (receive [message db chat-id]
    ))

(defrecord Seen [message-ids]
  (send [message db chat-id]
    ))

(defn get-topic [chat-id]
  (subs (web3.utils/sha3 chat-id) 0 10))

(defn require-ack [cofx message-id]
  )

(defn send-ack []
  )

(defn add-to-pending-messages [cofx chat-id message-id message]
  )

(defn make-whisper-message [cofx]
  )

(defn send [message options]
  (let [[message-id serialized-message] (serialize message)])
  (cond-> {}
    (options :ack) {:db (assoc-in [:transport chat-id :pending] serialized-message)}
    (options :new-key)
    {:shh/post {:web3          (:web3 db)
                :message {:sig identity
                          :ttl ttl
                          :payload (serialize payload)
                          :topic (get-topic chat-it)}}}))

(defn recieve []
  (cond-> {}
    :ack {:db (update-in [:transport chat-id :ack] #(remove message-id %))}
    :new-key ))
