(ns status-im.data-store.realm.schemas.account.v22.chat
  (:require [status-im.ui.components.styles :refer [default-chat-color]]))

(def schema {:name       :chat
             :primaryKey :chat-id
             :properties {:chat-id          :string
                          :name             :string
                          :color            {:type    :string
                                             :default default-chat-color}
                          :group-chat       {:type    :bool
                                             :indexed true}
                          :group-admin      {:type     :string
                                             :optional true}
                          :is-active        :bool
                          :timestamp        :int
                          :contacts         {:type       :list
                                             :objectType :chat-contact}
                          :unremovable?     {:type     :bool
                                             :default  false}
                          :removed-at       {:type     :int
                                             :optional true}
                          :removed-from-at  {:type     :int
                                             :optional true}
                          :added-to-at      {:type     :int
                                             :optional true}
                          :updated-at       {:type     :int
                                             :optional true}
                          :public-key       {:type     :string
                                             :optional true}
                          :private-key      {:type     :string
                                             :optional true}
                          :contact-info     {:type     :string
                                             :optional true}
                          :debug?           {:type    :bool
                                             :default false}
                          :public?          {:type    :bool
                                             :default false}}})
