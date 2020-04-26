(ns org-parser.parser-test
  (:require [org-parser.parser :as parser]
            [instaparse.core :as insta]
            #?(:clj [clojure.test :as t :refer :all]
               :cljs [cljs.test :as t :include-macros true])))


;; if parse is successful it returns a vector otherwise a map


(deftest word
  (let [parse #(parser/org % :start :word)]
    (testing "single"
      (is (= ["a"]
             (parse "a"))))
    (testing "single with trailing space"
      (is (map? (parse "ab "))))
    (testing "single with trailing newline"
      (is (map? (parse "a\n"))))))


(deftest tags
  (let [parse #(parser/org % :start :tags)]
    (testing "single"
      (is (= [:tags "a"]
             (parse ":a:"))))
    (testing "multiple"
      (is (= [:tags "a" "b" "c"]
             (parse ":a:b:c:"))))
    (testing "with all edge characters"
      (is (= [:tags "az" "AZ" "09" "_@#%"]
             (parse ":az:AZ:09:_@#%:"))))))


(deftest headline
  (let [parse #(parser/org % :start :head-line)]
    (testing "boring"
      (is (= [:head-line [:stars "*"] [:title "hello" "world"]]
             (parse "* hello world"))))
    (testing "with priority"
      (is (= [:head-line [:stars "**"] [:priority "A"] [:title "hello" "world"]]
             (parse "** [#A] hello world"))))
    (testing "with tags"
      (is (= [:head-line [:stars "***"] [:title "hello" "world"] [:tags "the" "end"]]
             (parse "*** hello world :the:end:"))))
    (testing "with priority and tags"
      (is (= [:head-line [:stars "****"] [:priority "B"] [:title "hello" "world"] [:tags "the" "end"]]
             (parse "**** [#B] hello world :the:end:"))))
    (testing "title cannot have multiple lines"
      (is (map? (parse "* a\nb"))))
    (testing "with comment flag"
      (is (= [:head-line [:stars "*****"] [:comment-token] [:title "hello" "world"]]
             (parse "***** COMMENT hello world"))))))


;; (deftest content
;;   (let [parse #(parser/org % :start :content-line)]
;;     (testing "boring"
;;       (is (= [[:content-line "anything"]
;;               [:content-line "goes"]]
;;              (parse "anything\ngoes"))))))


(deftest sections
  (let [parse parser/org]
    (testing "boring"
      (is (= [:S
              [:head-line [:stars "*"] [:title "hello" "world"]]
              [:content-line "this is the first section"]
              [:head-line [:stars "**"] [:title "and" "this"]]
              [:content-line "is another section"]]
             (parse "* hello world
this is the first section
** and this
is another section"))))
    (testing "boring with empty lines"
      (is (=[:S
             [:head-line [:stars "*"] [:title "hello" "world"]]
             [:content-line "this is the first section"]
             [:empty-line]
             [:head-line [:stars "**"] [:title "and" "this"]]
             [:empty-line]
             [:content-line "is another section"]]
            (parse "* hello world
this is the first section

** and this

is another section"))))))


(deftest affiliated-keyword
  (let [parse #(parser/org % :start :affiliated-keyword-line)]
    (testing "header"
      (is (= [:affiliated-keyword-line [:key "HEADER"] [:value "hello world"]]
             (parse "#+HEADER: hello world"))))
    (testing "name"
      (is (= [:affiliated-keyword-line [:key "NAME"] [:value "hello world"]]
             (parse "#+NAME: hello world"))))
    (testing "PLOT"
      (is (= [:affiliated-keyword-line [:key "PLOT"] [:value "hello world"]]
             (parse "#+PLOT: hello world"))))
    (testing "results"
      (is (= [:affiliated-keyword-line [:key "RESULTS"] [:value "hello world"]]
             (parse "#+RESULTS: hello world"))))
    (testing "results"
      (is (= [:affiliated-keyword-line [:key "RESULTS" [:optional "asdf"]] [:value "hello world"]]
             (parse "#+RESULTS[asdf]: hello world"))))
    (testing "caption"
      (is (= [:affiliated-keyword-line [:key "CAPTION"] [:value "hello world"]]
             (parse "#+CAPTION: hello world"))))
    (testing "caption"
      (is (= [:affiliated-keyword-line [:key "CAPTION" [:optional "qwerty"]] [:value "hello world"]]
             (parse "#+CAPTION[qwerty]: hello world"))))))


;; this is a special case of in-buffer-settings
(deftest todo
  (let [parse #(parser/org % :start :todo-line)]
    (testing "todos"
      (is (= [:todo-line [:todo-state "TODO"] [:done-state "DONE"]]
             (parse "#+TODO: TODO | DONE"))))))


(deftest greater-block-begin
  (let [parse #(parser/org % :start :greater-block-begin-line)]
    (testing "greater-block-begin"
      (is (= [:greater-block-begin-line [:greater-block-name "CENTER"] [:greater-block-parameters "some params"]]
             (parse "#+BEGIN_CENTER some params"))))))


(deftest greater-block-end
  (let [parse #(parser/org % :start :greater-block-end-line)]
    (testing "greater-block-end"
      (is (= [:greater-block-end-line [:greater-block-name "CENTER"]]
             (parse "#+END_CENTER"))))))


(deftest drawer-begin
  (let [parse #(parser/org % :start :drawer-begin-line)]
    (testing "drawer-begin"
      (is (= [:drawer-begin-line [:drawer-name "SOMENAME"]]
             (parse ":SOMENAME:"))))))


(deftest drawer-end
  (let [parse #(parser/org % :start :drawer-end-line)]
    (testing "drawer-end"
      (is (= [:drawer-end-line]
             (parse ":END:"))))))


(deftest dynamic-block-begin
  (let [parse #(parser/org % :start :dynamic-block-begin-line)]
    (testing "dynamic-block-begin"
      (is (= [:dynamic-block-begin-line [:dynamic-block-name "SOMENAME"] [:dynamic-block-parameters "some params"]]
             (parse "#+BEGIN: SOMENAME some params"))))))


(deftest dynamic-block-end
  (let [parse #(parser/org % :start :dynamic-block-end-line)]
    (testing "dynamic-block-end"
      (is (= [:dynamic-block-end-line]
             (parse "#+END:"))))))


(deftest footnote
  (let [parse #(parser/org % :start :footnote-line)]
    (testing "footnote with fn label"
      (is (= [:footnote-line [:footnote-label "some-label"] [:footnote-contents "some contents"]]
             (parse "[fn:some-label] some contents"))))
    (testing "footnote with number label"
      (is (= [:footnote-line [:footnote-label "123"] [:footnote-contents "some contents"]]
             (parse "[123] some contents"))))))


(deftest list-item-line
  (let [parse #(parser/org % :start :list-item-line)]

    (testing "list-item-line with asterisk"
      (is (= [:list-item-line [:list-item-bullet "*"] [:list-item-contents "a simple list item"]]
             (parse "* a simple list item"))))
    (testing "list-item-line with hyphen"
      (is (= [:list-item-line [:list-item-bullet "-"] [:list-item-contents "a simple list item"]]
             (parse "- a simple list item"))))
    (testing "list-item-line with plus sign"
      (is (= [:list-item-line [:list-item-bullet "+"] [:list-item-contents "a simple list item"]]
             (parse "+ a simple list item"))))
    (testing "list-item-line with counter and dot"
      (is (= [:list-item-line
              [:list-item-counter "1"]
              [:list-item-counter-suffix "."]
              [:list-item-contents "a simple list item"]]
             (parse "1. a simple list item"))))
    (testing "list-item-line with counter and parentheses"
      (is (= [:list-item-line
              [:list-item-counter "1"]
              [:list-item-counter-suffix ")"]
              [:list-item-contents "a simple list item"]]
             (parse "1) a simple list item"))))
    (testing "list-item-line with alphabetical counter and parentheses"
      (is (= [:list-item-line
              [:list-item-counter "a"]
              [:list-item-counter-suffix ")"]
              [:list-item-contents "a simple list item"]]
             (parse "a) a simple list item"))))
    (testing "list-item-line with alphabetical counter and parentheses"
      (is (= [:list-item-line
              [:list-item-counter "A"]
              [:list-item-counter-suffix ")"]
              [:list-item-contents "a simple list item"]]
             (parse "A) a simple list item"))))
    (testing "list-item-line with checkbox"
      (is (= [:list-item-line
              [:list-item-bullet "-"]
              [:list-item-checkbox [:list-item-checkbox-state "X"]]
              [:list-item-contents "a simple list item"]]
             (parse "- [X] a simple list item"))))
    ))


(deftest keyword
  (let [parse #(parser/org % :start :keyword-line)]
    (testing "keyword"
      (is (= [:keyword-line [:keyword-key "HELLO"] [:keyword-value "hello world"]]
             (parse "#+HELLO: hello world"))))))


(deftest node-property
  (let [parse #(parser/org % :start :node-property-line)]
    (testing "node-property"
      (is (= [:node-property-line [:node-property-name "HELLO"]]
             (parse ":HELLO:"))))
    (testing "node-property"
      (is (= [:node-property-line [:node-property-name "HELLO"] [:node-property-plus]]
             (parse ":HELLO+:"))))
    (testing "node-property"
      (is (= [:node-property-line
              [:node-property-name "HELLO"]
              [:node-property-value "hello world"]]
             (parse ":HELLO: hello world"))))
    (testing "node-property"
      (is (= [:node-property-line
              [:node-property-name "HELLO"]
              [:node-property-plus]
              [:node-property-value "hello world"]]
             (parse ":HELLO+: hello world"))))
    ))

(deftest timestamp
  (let [parse #(parser/org % :start :timestamp)]
    (testing "date timestamp without day"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-18"]] [:ts-modifiers]]]]
             (parse "<2020-01-18>"))))
    (testing "date timestamp with day"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-18"] [:ts-day "Sat"]] [:ts-modifiers]]]]
             (parse "<2020-01-18 Sat>"))))
    (testing "date timestamp with day in other language"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-21"] [:ts-day "Di"]] [:ts-modifiers]]]]
             (parse "<2020-01-21 Di>"))))
    (testing "date timestamp with day containing umlauts"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-21"] [:ts-day "Dönerstag"]] [:ts-modifiers]]]]
             (parse "<2020-01-21 Dönerstag>"))))
    (testing "date timestamp without day and time"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-time "12:00"]] [:ts-modifiers]]]]
             (parse "<2020-01-18 12:00>"))))
    (testing "date timestamp with day and time"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-day "Sat"] [:ts-time "12:00"]] [:ts-modifiers]]]]
             (parse "<2020-01-18 Sat 12:00>"))))
    (testing "date timestamp with day and time with seconds"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-day "Sat"] [:ts-time "12:00:00"]] [:ts-modifiers]]]]
             (parse "<2020-01-18 Sat 12:00:00>"))))

    (testing "timestamp with repeater"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-18"]]
                                             [:ts-modifiers [:ts-repeater [:ts-repeater-type "+"]
                                                             [:ts-mod-value "1"] [:ts-mod-unit "w"]]]]]]
             (parse "<2020-01-18 +1w>"))))
    (testing "timestamp with warning"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-18"]]
                                             [:ts-modifiers [:ts-warning [:ts-warning-type "-"]
                                                             [:ts-mod-value "2"] [:ts-mod-unit "d"]]]]]]
             (parse "<2020-01-18 -2d>"))))
    (testing "timestamp with both repeater and warning"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-18"]]
                                             [:ts-modifiers [:ts-repeater [:ts-repeater-type "+"]
                                                             [:ts-mod-value "1"] [:ts-mod-unit "w"]]
                                              [:ts-warning [:ts-warning-type "-"]
                                               [:ts-mod-value "2"] [:ts-mod-unit "d"]]]]]]
             (parse "<2020-01-18 +1w -2d>"))))
    (testing "timestamp with both warning and repeater"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-wo-time [:ts-date "2020-01-18"]]
                                             [:ts-modifiers [:ts-warning [:ts-warning-type "-"] [:ts-mod-value "2"] [:ts-mod-unit "d"]]
                                              [:ts-repeater [:ts-repeater-type "+"] [:ts-mod-value "1"] [:ts-mod-unit "w"]]]]]]
             (parse "<2020-01-18 -2d +1w>"))))
    (testing "timestamp with time and both warning and repeater"
      (is (= [:timestamp [:timestamp-active [:ts-inner
	     [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-time "18:00"]]
	     [:ts-modifiers
	      [:ts-warning [:ts-warning-type "-"] [:ts-mod-value "2"] [:ts-mod-unit "d"]]
	      [:ts-repeater [:ts-repeater-type "+"] [:ts-mod-value "1"] [:ts-mod-unit "w"]]]]]]
             (parse "<2020-01-18 18:00 -2d +1w>"))))

    (testing "timestamp with time span and both warning and repeater"
      (is (= [:timestamp [:timestamp-active [:ts-inner-span
	     [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-time "18:00"]]
	     [:ts-time "20:00"]
	     [:ts-modifiers
	      [:ts-warning [:ts-warning-type "-"] [:ts-mod-value "2"] [:ts-mod-unit "d"]]
	      [:ts-repeater [:ts-repeater-type "+"] [:ts-mod-value "1"] [:ts-mod-unit "w"]]]]]]
             (parse "<2020-01-18 18:00-20:00 -2d +1w>"))))

    (testing "more than one space between parts of timestamp does not matter"
      (is (= [:timestamp [:timestamp-active [:ts-inner
	     [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-time "18:00"]]
	     [:ts-modifiers
	      [:ts-warning [:ts-warning-type "-"] [:ts-mod-value "2"] [:ts-mod-unit "d"]]
	      [:ts-repeater [:ts-repeater-type "+"] [:ts-mod-value "1"] [:ts-mod-unit "w"]]]]]]
             (parse "<2020-01-18    18:00    -2d    +1w>"))))

    (testing "timestamp ranges"
      (is (= [:timestamp [:timestamp-active
	     [:ts-inner [:ts-inner-wo-time [:ts-date "2020-04-25"]] [:ts-modifiers]]
	     [:ts-inner [:ts-inner-wo-time [:ts-date "2020-04-28"]] [:ts-modifiers]]]]
             (parse "<2020-04-25>--<2020-04-28>"))))
    (testing "timestamp ranges with times"
      (is (= [:timestamp [:timestamp-active
	     [:ts-inner [:ts-inner-w-time [:ts-date "2020-04-25"] [:ts-time "08:00"]] [:ts-modifiers]]
	     [:ts-inner [:ts-inner-w-time [:ts-date "2020-04-28"] [:ts-time "16:00"]] [:ts-modifiers]]]]
             (parse "<2020-04-25 08:00>--<2020-04-28 16:00>"))))

    (testing "inactive timestamps"
      (is (= [:timestamp [:timestamp-inactive [:ts-inner-span
	     [:ts-inner-w-time [:ts-date "2020-01-18"] [:ts-time "18:00"]]
	     [:ts-time "20:00"]
	     [:ts-modifiers
	      [:ts-warning [:ts-warning-type "-"] [:ts-mod-value "2"] [:ts-mod-unit "d"]]
	      [:ts-repeater [:ts-repeater-type "+"] [:ts-mod-value "1"] [:ts-mod-unit "w"]]]]]]
             (parse "[2020-01-18 18:00-20:00 -2d +1w]"))))

    (testing "syntactically wrong timestamp"
      (is (insta/failure? (parse "<2020-04-25 day wrong>"))))

    (testing "at-least modifier for habits"
      (is (= [:timestamp [:timestamp-active [:ts-inner
	     [:ts-inner-wo-time [:ts-date "2009-10-17"] [:ts-day "Sat"]]
	     [:ts-modifiers [:ts-repeater
	       [:ts-repeater-type ".+"] [:ts-mod-value "2"] [:ts-mod-unit "d"]
	       [:ts-mod-at-least [:ts-mod-value "4"] [:ts-mod-unit "d"]]]]]]]
             (parse "<2009-10-17 Sat .+2d/4d>"))))

    (testing "accept seconds in time"
      (is (= [:timestamp [:timestamp-active [:ts-inner [:ts-inner-w-time
	      [:ts-date "2009-10-17"] [:ts-day "Sat"] [:ts-time "15:30:55"]] [:ts-modifiers]]]]
             (parse "<2009-10-17 Sat 15:30:55>"))))

    (testing "missing leading zeros in time are no problem"
      (is (= [:timestamp [:timestamp-active [:ts-inner
	     [:ts-inner-w-time [:ts-date "2009-10-17"] [:ts-day "Sat"] [:ts-time "8:00"]] [:ts-modifiers]]]]
             (parse "<2009-10-17 Sat 8:00>"))))

    (testing "newlines are not recognized as space \\s"
      ;; http://xahlee.info/clojure/clojure_instaparse.html
      (is (insta/failure? (parse "<2020-04-17 F\nri>"))))))


(deftest literal-line
  (let [parse #(parser/org % :start :literal-line)]
    (testing "parse literal line starting with colon"
      (is (= [:literal-line [:ll-leading-space ""] [:ll-text "literal text"]]
             (parse ":literal text"))))
    (testing "parse literal line starting with spaces"
      (is (= [:literal-line [:ll-leading-space "  "] [:ll-text " literal text "]]
             (parse "  : literal text "))))))

(deftest links
  (let [parse #(parser/org % :start :link-format)]
    (testing "parse simple link"
      (is (= [:link-format [:link [:link-ext [:link-ext-other
	      [:link-url-scheme "https"]
	      [:link-url-rest "//example.com"]]]]]
             (parse "[[https://example.com]]"))))
    (testing "parse simple link without protocol"
      (is (= [:link-format [:link [:link-ext [:link-ext-web "www.example.com"]]]]
             (parse "[[www.example.com]]"))))
    (testing "parse link with description"
      (is (= [:link-format [:link [:link-ext [:link-ext-other
	      [:link-url-scheme "https"]
	      [:link-url-rest "//example.com"]]]]
	     [:link-description "description words"]]
             (parse "[[https://example.com][description words]]"))))
    (testing "parse interal * link"
      (is (= [:link-format [:link [:link-int [:link-file-loc-customid "my-custom-id"]]]]
             (parse "[[#my-custom-id]]"))))
    (testing "parse interal # link"
      (is (= [:link-format [:link [:link-int [:link-file-loc-headline "My Header"]]]]
             (parse "[[*My Header]]"))))
    (testing "parse interal link"
      (is (= [:link-format [:link [:link-int [:link-file-loc-string "A Name"]]]]
             (parse "[[A Name]]"))))))

(deftest links-external-file
  (let [parse #(parser/org % :start :link-ext-file)]
    (testing "parse file link"
      (is (= [:link-ext-file "folder/file.txt"]
             (parse "file:folder/file.txt"))))
    (testing "parse file link"
      (is (= [:link-ext-file "~/folder/file.txt"]
             (parse "file:~/folder/file.txt"))))
    (testing "parse relative file link"
      (is (= [:link-ext-file "./folder/file.txt"]
             (parse "./folder/file.txt"))))
    (testing "parse absolute file link"
      (is (= [:link-ext-file "/folder/file.txt"]
             (parse "/folder/file.txt"))))
    (testing "parse file link with line number"
      (is (= [:link-ext-file "./file.org" [:link-file-loc-lnum "15"]]
             (parse "./file.org::15"))))
    (testing "parse file link with text search string"
      (is (= [:link-ext-file "./file.org" [:link-file-loc-string "foo bar"]]
             (parse "./file.org::foo bar"))))
    (testing "parse file link with headline"
      (is (= [:link-ext-file "./file.org" [:link-file-loc-headline "header1: test"]]
             (parse "./file.org::*header1: test"))))
    (testing "parse file link with custom id"
      (is (= [:link-ext-file "./file.org" [:link-file-loc-customid "custom-id"]]
             (parse "./file.org::#custom-id"))))))

(deftest links-external-other-url
  (let [parse #(parser/org % :start :link-ext-other)]
    (testing "parse other http link"
      (is (= [:link-ext-other [:link-url-scheme "https"] [:link-url-rest "//example.com"]]
             (parse "https://example.com"))))
    (testing "parse other mailto link"
      (is (= [:link-ext-other [:link-url-scheme "mailto"] [:link-url-rest "info@example.com"]]
             (parse "mailto:info@example.com"))))
    (testing "parse other link with uncommon scheme"
      (is (= [:link-ext-other [:link-url-scheme "zyx"] [:link-url-rest "rest-of uri ..."]]
             (parse "zyx:rest-of uri ..."))))))

(deftest links-external-web-address
  (let [parse #(parser/org % :start :link-ext-web)]
    (testing "parse web address without scheme"
      (is (= [:link-ext-web "www.example.com"]
             (parse "www.example.com"))))
    (testing "parse web address without scheme"
      (is (= [:link-ext-web "a.subdomain.example.com"]
             (parse "a.subdomain.example.com"))))
    (testing "parse web address without scheme"
      (is (= [:link-ext-web "example.com/list?filter=abc&sort=Desc"]
             (parse "example.com/list?filter=abc&sort=Desc"))))))
