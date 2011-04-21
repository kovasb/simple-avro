(ns simple-avro.schema
  {:doc "Avro 1.5 schema specification.
        See http://avro.apache.org/docs/1.5.0/spec.html for details."}
  (:use (clojure.contrib [string :only (as-str)]
                         [json :only (json-str)])))

(defmacro avro-type
  "Standard type declaration."
  [type]
  `{:type ~type})

; Primitive types

(defmacro avro-prim
  "Primitive types declaration helper."
  {:private true}
  [type]
  `(def ~(symbol (str "avro-" type))
     (avro-type ~(str type))))

; Primitive types
(avro-prim null)
(avro-prim boolean)
(avro-prim int)
(avro-prim long)
(avro-prim float)
(avro-prim double)
(avro-prim bytes)
(avro-prim string)

; Complex types

(defn avro-array
  [type]
  {:type "array" :items type})

(defn avro-map
  [type]
  {:type "map" :values type})

(defn avro-union
  [ & types]
  (vec types))

; Named types

(defn- sanitize-name
  "Basic string name sanitization.
  The result is the name in camelcase notation."
  [name]
  (loop [n name rs [] upper true]
    (if (empty? n)
      (apply str rs)
      (let [#^Character f (first n)]
        (cond
          (Character/isLetterOrDigit f)
            (recur (next n) (conj rs (if upper (Character/toUpperCase f) f)) false)
          (= f \_)
            (recur (next n) (conj rs \_) true)
          :else 
            (recur (next n) rs true))))))

(defn avro-record
  [name & decl]
  (let [schema        {:type "record"
                       :name (sanitize-name name)}
        [schema decl] (if (map? (first decl))
                        [(merge schema (first decl)) (next decl)]
                        [schema decl])]
    (assoc schema :fields
      (loop [d decl fields []]
        (if (empty? d)
          fields
          (let [[field d] [{:name (as-str (first d))
                            :type (first (next d))}
                           (drop 2 d)]
                [field d] (if (map? (first d))
                            [(merge (first d) field) (next d)]
                            [field d])]
            (recur d (conj fields field))))))))

(defn avro-enum
  [name & decl]
  (let [schema        {:type "enum"
                       :name (sanitize-name name)}
        [schema decl] (if (map? (first decl))
                        [(merge schema (first decl)) (next decl)]
                        [schema decl])]
    (assoc schema :symbols (vec (clojure.core/map as-str decl)))))

(defn avro-fixed
  [name size & [opts]]
  (let [schema {:type "fixed"
                :size size
                :name (sanitize-name name)}]
    (if opts
      (merge opts schema)
      schema)))

; Convenient named type declaration macros

(defmacro defavro-record
  [name & decl]
  `(def ~name
     (avro-record ~(str name) ~@decl)))

(defmacro defavro-enum
  [name & decl]
  `(def ~name
     (avro-enum ~(str name) ~@decl)))


(defmacro defavro-fixed
  [name size & [opts]]
  `(def ~name
     (avro-fixed ~(str name) ~size ~opts)))

