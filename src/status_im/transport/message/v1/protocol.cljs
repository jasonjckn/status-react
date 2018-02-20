(ns status-im.transport.message.v1.protocol
  (:require [status-im.transport.utils :as transport.utils]
            [status-im.transport.message-cache :as message-cache]
            [status-im.transport.db :as transport.db]
            [status-im.transport.core :as transport]))

(def ttl (* 3600 1000)) ;; ttl of one hour

(defn message-id [message]
  (transport.utils/sha3 (pr-str message)))

(defn get-topic [chat-id]
  (subs (transport.utils/sha3 chat-id) 0 10))

(defn init-chat [cofx chat-id]
  (assoc-in cofx [:transport/chats chat-id] (transport.db/create-chat (get-topic chat-id))))

(defn is-new? [message-id]
  (when-not (message-cache/exists? message-id)
    (message-cache/add! message-id)))

(defn require-ack [cofx message-id chat-id]
  (update-in cofx [:db :transport/chats chat-id :waiting-ack] conj message-id))

(defn ack [cofx message-id chat-id]
  (update-in cofx [:db :transport/chats chat-id :ack] conj message-id))

(defn send [{:keys [db]} {:keys [payload chat-id]}]
  ;; we assume that the chat contains the contact public-key
  (let [{:accounts/keys [account]} db
        {:keys [identity]} account
        {:keys [sym-key-id]} (get-in db [:transport chat-id])]
    {:shh/post {:web3    (:web3 db)
                :message {:sig identity
                          :symKeyId sym-key-id
                          :ttl ttl
                          :powTarget 0.001
                          :powTime 1
                          :payload (serialize payload)
                          :topic (get-topic chat-id)}}}))

(defn send-with-pubkey [{:keys [db]} {:keys [payload chat-id]}]
  (let [{:accounts/keys [account]} db
        {:keys [identity]} account]
    {:shh/post {:web3    (:web3 db)
                :message {:sig identity
                          :pubKey chat-id
                          :ttl ttl
                          :powTarget 0.001
                          :powTime 1
                          :payload (serialize payload)
                          :topic (get-topic chat-id)}}}))

(defrecord Ack [message-ids]
  message/StatusMessage
  (send [this cofx chat-id])
  (receive [this db chat-id sig]))

(defrecord Seen [message-ids]
  message/StatusMessage
  (send [this cofx chat-id])
  (receive [this cofx chat0id sig]))
