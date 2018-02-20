(ns status-im.transport.db
  (:require [cljs.spec.alpha :as spec]))

(s/def ::topic string?)
(s/def ::sym-key-id string?)
(s/def ::seen (spec/coll-of string? :kind vector?))
(s/def ::ack (spec/coll-of string? :kind vector?))

(s/def :transport/chat (spec/keys :req [::ack ::seen ::pending-ack ::pending-send ::topic]
                                  :opt [::sym-key-id]))

(s/def :transport/chats (spec/map-of string? :transport/chat))


(defn create-chat [topic]
  {:ack []
   :seen []
   :pending-ack []
   :pending-messages []
   :topic topic})
