(ns socialsite.core
  (:refer-clojure :exclude [take drop sort distinct compile conj! disj!])
  (:use clojureql.core
        net.cgrand.moustache
        ring.util.response
        ring.adapter.jetty
        [socialsite templates]
        [ring.middleware file params reload session]))

(def db   {:classname   "com.mysql.jdbc.Driver"
           :subprotocol "mysql"
           :user        "social"
           :password    "social"
           :subname     "//localhost:3306/social"})

(def posts (table db :posts))
(def users (table db :users))

(defn view-frontpage
  [r]
  (->> (frontpage @(-> (join posts (project users [:nick])
                           (where (= :posts.submitter :users.id)))
                     (sort [:upvotes#desc])))
       (page (:session r) ["/css/frontpage.css"] nil)
       response))

(defn view-submission-page
  [r]
  (->> (page (:session r) ["/css/submit.css"] nil (submit))
       response))

(defn view-login-page
  [r]
  (->> (page nil ["/css/login.css"] nil (login))
       response))

(defn vote
  [{id :form-params} direction]
  (let [predicate (where (= :id (get id "id")))]
    (update-in! posts (where predicate)
     (if (= "up" direction)
       {:upvotes   (inc (-> (select posts (where predicate))
                            (pick! :upvotes)))}
       {:downvotes (inc (-> (select posts (where predicate))
                            (pick! :downvotes)))}))
    (-> (format "OK: %s" (-> (select posts (where predicate))
                             (aggregate [["(upvotes - downvotes)" :as :sum]])
                             (pick! :sum)))
        response)))

(defn accept-submission
  [req]
  (let [params (-> req :form-params)
        user   (-> req :session :id)]
    (conj! posts {:title     (params "title")
                  :url       (params "url")
                  :submitter user})
    (response "OK")))

(defn authenticate-user
  [{params :form-params}]
  (let [user  @(select users (where (and (= :nick     (params "usr"))
                                         (= :password (params "psw")))))]
    (if (= 1 (count user))
      (assoc (redirect "/") :session (first user))
      (redirect "/login?err=true"))))

(defn logout
  [r]
  (assoc (redirect "http://www.bestinclass.dk")
    :session nil))

(defn wrap-security
  [app]
  (fn [req]
    (let [uri (:uri req)]
      (if (or (-> req :session :nick seq)
              (= uri "/")
              (= uri "/login"))
        (app req)
        (redirect "/?err=Not logged in")))))

(def routes
  (app
   (wrap-file   "resources")
   (wrap-reload '[socialsite.templates])
   (wrap-session)
   (wrap-security)

   [""]               view-frontpage
   ["logout"]         logout
   ["login"]          {:get view-login-page
                       :post (wrap-params authenticate-user)}
   ["submit"]         {:get  view-submission-page
                       :post (wrap-params accept-submission)}
   ["vote" direction] (wrap-params (delegate vote direction))))

(doto (Thread. #(run-jetty #'routes {:port 8080})) .start)