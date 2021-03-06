package org.cryptimeleon.math.serialization.annotations.internal;

import org.cryptimeleon.math.serialization.ListRepresentation;
import org.cryptimeleon.math.serialization.Representation;
import org.cryptimeleon.math.serialization.annotations.RepresentationRestorer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

/**
 * A handler for serializing/deserializing {@link List} and {@link Set} instances.
 */
public class ListAndSetRepresentationHandler implements RepresentationHandler {
    private static final Class<?>[] supportedFallbackClasses = new Class[] {ArrayList.class, HashSet.class};

    /**
     * Handler for the list/set elements.
     */
    protected RepresentationHandler elementHandler;

    /**
     * Class of the list/set object.
     */
    protected Class<?> collectionType;

    /**
     * Type of the list/set elements.
     */
    protected Type elementType;

    public ListAndSetRepresentationHandler(RepresentationHandler elementHandler, Type collectionType) {
        this.elementHandler = elementHandler;
        this.collectionType = (Class<?>) ((ParameterizedType) collectionType).getRawType();
        this.elementType = getElementType(collectionType);
    }

    /**
     * Retrieves the type of the elements of the given collection type.
     * @param collectionType the type of the collection
     * @return the type of the elements of the collection type
     */
    public static Type getElementType(Type collectionType) {
        Type[] typeArguments = ((ParameterizedType) collectionType).getActualTypeArguments();
        if (typeArguments.length != 1) {
            throw new IllegalArgumentException("Cannot handle collections with more than one generic type");
        }
        return typeArguments[0];
    }

    /**
     * Checks whether this handler can handle lists/sets of the given type.
     * @param collectionType the type to check
     * @return true if this handler can handle the given type, else false
     */
    public static boolean canHandle(Type collectionType) { //handles List|Set<anything>.
        if (!(collectionType instanceof ParameterizedType))
            return false;

        Type rawType = ((ParameterizedType) collectionType).getRawType();

        if (!(rawType instanceof Class))
            return false;

        if (!List.class.isAssignableFrom((Class<?>) rawType) && !Set.class.isAssignableFrom((Class<?>) rawType))
            return false;

        try { //Check if generic type argument is okay
            getElementType(collectionType);
        } catch (IllegalArgumentException e) {
            return false;
        }

        //Check if the collection has a default constructor or is an interface.
        try {
            ((Class<?>) rawType).getConstructor();
        } catch (NoSuchMethodException e) {
            // if the type is an interface, we try the defined fallback classes
            return ((Class<?>) rawType).isInterface()
                    && Arrays.stream(supportedFallbackClasses)
                             .anyMatch(((Class<?>) rawType)::isAssignableFrom);
        }

        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Object deserializeFromRepresentation(Representation repr, Function<String, RepresentationRestorer> getRegisteredRestorer) {
        if (repr == null)
            return null;

        Collection result = null;

        //Try to call default constructor to create collection.
        try {
            result = (Collection) collectionType.getConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            // if the type is an interface, we try the defined fallback classes
            // for example, fall back to ArrayList if the type is List
            for (Class<?> fallback : supportedFallbackClasses) {
                try {
                    if (collectionType.isAssignableFrom(fallback))
                        result = (Collection) fallback.getConstructor().newInstance();
                } catch (InstantiationException | NoSuchMethodException | IllegalAccessException ex) {
                    throw new RuntimeException("Cannot instantiate type "+collectionType.getName());
                } catch (InvocationTargetException ex) {
                    if (e.getCause() instanceof RuntimeException)
                        throw (RuntimeException) ex.getCause();
                    else
                        throw new RuntimeException("An error occured during invocation of the constructor of "+fallback.getSimpleName(), ex);
                }
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException)
                throw (RuntimeException) e.getCause();
            else
                throw new RuntimeException("An error occured during invocation of the constructor of "+collectionType.getSimpleName(), e);
        }

        if (result == null)
            throw new RuntimeException("Cannot instantiate type "+collectionType.getName());

        //Restore elements
        for (Representation inner : repr.list())
            result.add(elementHandler.deserializeFromRepresentation(inner, getRegisteredRestorer));

        return result;
    }

    @Override
    public Representation serializeToRepresentation(Object obj) {
        if (obj == null)
            return null;

        if (!(obj instanceof List || obj instanceof Set))
            throw new IllegalArgumentException("Cannot handle representation of "+obj.getClass().getName());

        if (!(obj instanceof List)) { //shuffle elements to avoid order leak
            obj = Arrays.asList(((Set<?>) obj).toArray());
            Collections.shuffle((List<?>) obj);
        }

        ListRepresentation repr = new ListRepresentation();
        for (Object inner : (Iterable<?>) obj) {
            repr.put(elementHandler.serializeToRepresentation(inner));
        }
        return repr;
    }
}
