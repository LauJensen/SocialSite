(ns socialsite.templates
  (:use net.cgrand.enlive-html))

(defsnippet login "login.html" [:body :> any-node] [])
(defsnippet submit "submit.html" [:body :> any-node] [])

(defsnippet frontpage "frontpage.html" [:body :> any-node]
  [posts]
  [:div.post]
  (clone-for [{:keys [id title url nick upvotes downvotes]} posts]
    [:div.voting]    (set-attr :pid (str id))
    [:div.votes]     (-> (- upvotes downvotes) str content)
    [:div.title]     (content
                      {:tag :a, :attrs {:href url}, :content [title]})
    [:div.submitter] (-> (str "submitted by " nick) content)))

(deftemplate page "page.html" [session styles scripts cnt]
  [:link.style]   (clone-for [style styles]
                    (set-attr :href style))
  [:script.import] (clone-for [script scripts]
                    (set-attr :src script))
  [:a#loginout]   (content
                   (if (seq (:nick session))
                     {:tag :a, :attrs {:href "/logout"}, :content ["logout"]}
                     {:tag :a, :attrs {:href "/login"}, :content ["login"]}))
  [:div#content]  (content cnt))