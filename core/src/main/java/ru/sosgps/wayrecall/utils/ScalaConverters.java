package ru.sosgps.wayrecall.utils;

import scala.Option;
import scala.Predef;
import scala.collection.JavaConversions$;

import java.lang.Iterable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScalaConverters {

    public static <T> Iterable<T> asJavaIterable(scala.collection.Iterable<T> si){
        return scala.collection.JavaConversions$.MODULE$.asJavaIterable(si);
    }

    public static <T> Collection<T> asJavaCollection(scala.collection.Iterable<T> si){
        return scala.collection.JavaConversions$.MODULE$.asJavaCollection(si);
    }

    public static <K,V> Map<K,V> asJavaMap(scala.collection.Map<K,V> si){
        return scala.collection.JavaConversions$.MODULE$.mapAsJavaMap(si);
    }

    public static <K,V> Map<K,V> asImmutableJavaMap(scala.collection.immutable.Map<? extends K, ? extends V> si){
        return Collections.<K, V>unmodifiableMap(JavaConversions$.MODULE$.mapAsJavaMap(si));
    }

    public static <T> List<T> asJavaList(scala.collection.Seq<T> si){
        return scala.collection.JavaConversions$.MODULE$.seqAsJavaList(si);
    }

    public static <T> scala.collection.mutable.Buffer<T> asScalaBuffer(List<T> l){
        return scala.collection.JavaConversions$.MODULE$.asScalaBuffer(l);
    }

    public static <T> scala.collection.Iterable<T> asScalaIterable(Collection<T> l){
        return scala.collection.JavaConversions$.MODULE$.collectionAsScalaIterable(l);
    }

    public static <T> scala.collection.Iterable<T> asScalaIterable(Iterable<T> l){
        return scala.collection.JavaConversions$.MODULE$.iterableAsScalaIterable(l);
    }

    public static <K,V> scala.collection.mutable.Map<K,V> asScalaMap(Map<K,V> l){
        return scala.collection.JavaConversions$.MODULE$.mapAsScalaMap(l);
    }
    
    public static <K,V> scala.collection.immutable.Map<K,V> asScalaMapImm(Map<K,V> l){
        return new scala.collection.immutable.HashMap<K,V>().$plus$plus(asScalaMap(l));
    }

    public static <T> T nullable(Option<T> opt){
        if(opt.isDefined())
            return opt.get();
        else
            return null;
    }

}
