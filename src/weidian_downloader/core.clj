(ns weidian-downloader.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]
            [clj-webdriver.taxi :as tx]))

(def links '("<link to product page>"))

(def base-folder "<local working folder>")

(defn- write-img [img file-name]
  (let []
    (with-open [w (io/output-stream file-name)]
      (.write w img))))

(defn- write-info [price-element info-element file-name]
  (let [info (.getText (:webelement info-element))
        price (.getText (:webelement price-element))]
    (with-open [w (io/writer file-name :append true)]
      (.write w price)
      (.write w "\n")
      (.write w info))))

(defn download-files [urls]
  (tx/set-driver! {:browser :chrome})
  (doseq [[url index] (map list urls (range 0 (count urls)))]
    (tx/to url)
    (let [e (tx/find-element {:css "div.item-main-picture img"})
          img_link (.getAttribute (:webelement e) "src")
          img (:body (client/get img_link {:as :byte-array}))
          info-element (tx/find-element {:css "div.item-info-text"})
          price-element (tx/find-element {:css "span.j_realPrice"})
          ]
      (write-img img (str base-folder "img-" index ".jpg"))
      (write-info price-element info-element (str base-folder "info-" index ".txt"))))
  (tx/quit))

(defn- get-price-and-product-info [file-name]
  (with-open [rdr (io/reader file-name)]
    (let [lines (doall (line-seq rdr))]
      (list (nth lines 0) (clojure.string/join "\n" (rest lines))))
    ))

(defn- upload-product [img-path price description]
  (tx/implicit-wait 4000)
  (tx/wait-until #(tx/exists? "input.item_img_input"))
  (Thread/sleep 10000)
  (tx/send-keys {:css "input.item_img_input"} img-path)
  (tx/submit  {:css "input.item_img_input"})
  (tx/input-text {:css "textarea#i_des"} description)
  (tx/input-text {:css "input#i_no_sku_price"} price)
  (tx/input-text {:css "input#i_no_sku_stock"} "100")
  (tx/click {:css "a#submit_i_do_item"})
  (Thread/sleep 4000))

(defn upload-files []
  (tx/set-driver! {:browser :chrome})
  (tx/to "http://d.weidian.com/vshop/1/CPC/login.php")
  (tx/select {:css "#country option[value='61']"})
  (tx/input-text {:css "#tele"}  "<your mobile number>")
  (tx/click {:css "#login_submit"})
  (tx/implicit-wait 4000)
  (tx/input-text {:css "#pwd"} "<your password>")
  (tx/click {:css "a#login_submit"})
  (tx/click {:css "a.icon_myvdian"})
  (tx/click {:css "a#add_item"})
  (tx/implicit-wait 10000)
  (let [indexes (range 259 269)]
    (doseq [index indexes]
      (let [img-file-name (str base-folder "img-" index ".jpg")
            info-file-name (str base-folder "info-" index ".txt")
            product-info (get-price-and-product-info info-file-name)
            price (nth product-info 0)
            description (nth product-info 1)]
        (upload-product img-file-name price description))))
  (tx/quit))
