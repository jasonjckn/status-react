(ns status-im.transport.shh
  (:require [taoensso.timbre :as log]
            [re-frame.core :as re-frame]
            [cljs.spec.alpha :as s]
            [status-im.utils.handlers :as handlers]
            [status-im.transport.utils :as web3.utils]
            [taoensso.timbre :refer-macros [debug]]))

(defn get-new-key-pair [{:keys [web3 on-success on-error]}]
  (if web3
    (.. web3
        -shh
        (newKeyPair (fn [err resp]
                      (if-not err
                        (on-success resp)
                        (on-error err)))))
    (on-error "web3 not available.")))

(re-frame/reg-fx
  :shh/get-new-key-pair
  (fn [{:keys [web3 success-event error-event]}]
    (get-new-key-pair {:web3       web3
                       :on-success #(re-frame/dispatch [success-event %])
                       :on-error   #(re-frame/dispatch [error-event %])})))

(defn get-public-key [{:keys [web3 key-pair-id on-success on-error]}]
  (if (and web3 key-pair-id)
    (.. web3
        -shh
        (getPublicKey key-pair-id (fn [err resp]
                                    (if-not err
                                      (on-success resp)
                                      (on-error err)))))
    (on-error "web3 or key-pair id not available.")))

(re-frame/reg-fx
  :shh/get-public-key
  (fn [{:keys [web3 key-pair-id success-event error-event]}]
    (get-public-key {:web3       web3
                     :key-pair-id key-pair-id
                     :on-success #(re-frame/dispatch [success-event %])
                     :on-error   #(re-frame/dispatch [error-event %])})))

(defn generate-sym-key-from-password
  [{:keys [web3 password on-success on-error]}]
  (.. web3
      -shh
      (generateSymKeyFromPassword password (fn [err resp]
                                             (if-not err
                                               (on-success resp)
                                               (on-error err))))))

(re-frame/reg-fx
  :shh/generate-sym-key-from-password
  (fn [{:keys [web3 password success-event error-event]}]
    (generate-sym-key-from-password {:web3       web3
                                     :password   password
                                     :on-success #(re-frame/dispatch [success-event %])
                                     :on-error   #(re-frame/dispatch [error-event %])})))

(defn post-message
  [{:keys [web3 whisper-message on-success on-error]}]
  (.. web3
      -shh
      (post (clj->js whisper-message) (fn [err resp]
                                        (if-not err
                                          (on-success resp)
                                          (on-error err))))))

(re-frame/reg-fx
  :shh/post
  (fn [{:keys [web3 whisper-message success-event error-event]
        :or {success-event :protocol/send-status-message-success
             error-event   :protocol/send-status-message-error}}]
    (post-message {:web3       web3
                   :whisper-message    whisper-message
                   :on-success #(re-frame/dispatch [success-event %])
                   :on-error   #(re-frame/dispatch [error-event %])})))

;; TODO (yenda) do we need such a function ? How do we avoid too much repetition in message records ?
;; TODO (yenda) where do we deal with serialization deserialization ?
(defn prepare-whisper-message [message]
  (-> message
      (assoc :powTarget 0.001
             :powTime 1)
      (update :payload serialize)))
