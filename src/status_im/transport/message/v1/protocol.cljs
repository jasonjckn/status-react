(ns status-im.transport.message.v1.protocol)

(def ttl (* 3600 1000)) ;; ttl of one hour

(defn get-topic [chat-id]
  (subs (web3.utils/sha3 chat-id) 0 10))

(defn send [db {:keys [payload chat-id]}]
  ;; we assume that the chat contains the contact public-key
  (let [{:accounts/keys [account]} db
        {:keys [identity]} account
        {:keys [sym-key-id pending]} (get-in db [:transport chat-id])]
    {:shh/post {:web3    (:web3 db)
                :message {:sig identity
                          :symKeyId sym-key-id
                          :ttl ttl
                          :payload (serialize payload)
                          :topic (get-topic chat-id)}}}))

(defn send-with-pubkey [db {:keys [payload chat-id]}]
  {:shh/post {:web3    (:web3 db)
              :message {:sig identity
                        :pubKey public-key
                        :ttl ttl
                        :payload (serialize payload)
                        :topic (get-topic chat-id)}}})


(defrecord NewKey [password]
  (send [message cofx chat-id]
    (send-with-pubkey db {:chat-id chat-id
                          :payload (conj pending message)}))
  (receive [message db chat-id]
    {:generate-sym-key-from-password {:password password
                                      :chat-id chat-id}}))

(defrecord Ack [message-ids]
  (send [])
  (receive [message db chat-id]
    ))

(defrecord Seen [message-ids]
  (send [message db chat-id]
    ))

(defn generate-new-key-and-password [cofx chat-id]
  (-> cofx
      (assoc-in [:db :transport chat-id :pending] message)
      (assoc-in [:shh/generate-sym-key-and-password chat-id])))
