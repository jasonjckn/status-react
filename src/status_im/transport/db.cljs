(ns status-im.transport.db
  (:require [cljs.spec.alpha :as spec]))

(spec/def ::topic string?)
(spec/def ::sym-key-id string?)
(spec/def ::seen (spec/coll-of string? :kind vector?))
(spec/def ::ack (spec/coll-of string? :kind vector?))

(spec/def :transport/chat (spec/keys :req [::ack ::seen ::pending-ack ::pending-send ::topic]
                                     :opt [::sym-key-id]))

(spec/def :transport/chats (spec/map-of string? :transport/chat))


(defn create-chat [topic]
  {:ack []
   :seen []
   :pending-ack []
   :pending-messages []
   :topic topic})
