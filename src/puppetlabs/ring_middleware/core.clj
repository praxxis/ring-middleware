(ns puppetlabs.ring-middleware.core
  (:require [ring.middleware.cookies :refer [wrap-cookies]]
            [clojure.string :refer [join split]]
            [puppetlabs.http.client.sync :refer [request]]
            [puppetlabs.ring-middleware.common :refer [prepare-cookies slurp-binary]])
  (:import (java.net URI)))

(defn wrap-proxy
  "Proxies requests to proxied-path, a local URI, to the remote URI at
  remote-uri-base, also a string."
  [handler ^String proxied-path remote-uri-base & [http-opts]]
  (wrap-cookies
    (fn [req]
      (if (.startsWith ^String (:uri req) (str proxied-path "/"))
        (let [uri (URI. remote-uri-base)
              remote-uri (URI. (.getScheme uri)
                               (.getAuthority uri)
                               (str (.getPath uri)
                                    (subs (:uri req) (.length proxied-path)))
                               nil
                               nil)]
          (-> (merge {:method (:request-method req)
                      :url (str remote-uri "?" (:query-string req))
                      :headers (dissoc (:headers req) "host" "content-length")
                      :body (if-let [len (get-in req [:headers "content-length"])]
                              (slurp-binary (:body req) (Integer/parseInt len)))
                      :as :stream} http-opts)
              request
              prepare-cookies))
        (handler req)))))