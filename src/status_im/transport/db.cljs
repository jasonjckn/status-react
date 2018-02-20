(ns status-im.transport.db
  (:require [cljs.spec.alpha :as spec]))

(s/def ::topic string?)
(s/def ::sym-key-id string?)
(s/def ::seen (spec/coll-of string? :kind vector?))
(s/def ::ack (spec/coll-of string? :kind vector?))

(s/def :transport/chat (spec/keys :req [::ack ::seen ::pending ::topic]
                                  :opt [::sym-key-id]))

(s/def :transport/chats (spec/map-of string? :transport/chat))


(defn create-chat [chat-id]
  {:ack []
   :waiting-ack []
   :seen []
   :pending []})
