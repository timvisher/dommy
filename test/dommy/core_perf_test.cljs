(ns dommy.core-perf-test
  (:use-macros
   [dommy.macros :only [node sel sel1]])
  (:require
   [dommy.core :as dommy]))

(defn profile-fn [f]
  (let [now (js/performance.now)]
    (f)
    (- (js/performance.now) now)))

(defn perf-test [f samples]
  (let [results (repeatedly samples #(profile-fn f))
        total (apply + results)]
    {:results results
     :total total
     :mean (/ total samples)}))

(defn perf-test-map [fn-map samples]
  (->> fn-map
       (map (fn [[k f]] [k (perf-test f samples)]))
       (into {})
       clj->js))

(defn ^:export selector-perf-test [samples]
  (dommy/append! (sel1 :body) [:#c1 [:.c2 [:.c3]] [:.c3]])
  (let [c2 (sel1 :.c2)]
   (perf-test-map
    {:dommy-body #(sel1 :body)
     :jquery-body #(js/jQuery "body")
     :dommy-id #(sel1 :#c1)
     :jquery-id #(js/jQuery "#c1")
     :dommy-class-sel1 #(sel1 :.c3)
     :jquery-class-sel1 #(aget (js/jQuery ".c3") 0)
     :dommy-multi-class #(sel ".c2, .c3")
     :jquery-multi-class #(js/jQuery ".c2, .c3")
     :dommy-nested-sel1 #(sel1 c2 :.c3)
     :jquery-nested-sel1 #(js/jQuery ".c3" c2)}
    samples)))

(defn ^:export append-perf-test [samples]
  (let [dommy-container (node :div)
        jquery-container (js/jQuery (node :div))
        el (node :div)]
    (perf-test-map
     {:dommy #(dommy/append! dommy-container el)
      :jquery #(.append jquery-container el)}
     samples)))

(defn ^:export toggle-class-perf-test [samples]
  (let [el (node :div)
        jquery-el (js/jQuery el)]
    (perf-test-map
     {:dommy #(dommy/toggle-class! el :pwned)
      :jquery #(.toggleClass jquery-el "pwned")}
     samples)))

(defn ^:export listen-perf-test [samples]
  (let [el (node [:div [:.child]])
        jquery-el (js/jQuery el)
        noop #()]
    (perf-test-map
     {:naked-simple #(do (.addEventListener el "click" noop)
                         (.removeEventListener el "click" noop))
      :dommy-simple #(do (dommy/listen! el :click noop)
                         (dommy/unlisten! el :click noop))
      :jquery-simple #(do (.on jquery-el "click" noop)
                          (.off jquery-el "click" noop))}
     samples)))