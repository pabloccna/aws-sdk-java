/*
 * Copyright (c) 2016. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.codegen.internal;

import static com.amazonaws.codegen.internal.Constants.LOGGER;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.codegen.model.intermediate.IntermediateModel;
import com.amazonaws.codegen.model.intermediate.Protocol;
import com.amazonaws.codegen.model.intermediate.ShapeMarshaller;
import com.amazonaws.codegen.model.intermediate.ShapeModel;
import com.amazonaws.codegen.model.service.Input;
import com.amazonaws.codegen.model.service.Operation;
import com.amazonaws.codegen.model.service.ServiceMetadata;
import com.amazonaws.codegen.model.service.Shape;
import com.amazonaws.codegen.model.service.XmlNamespace;
import com.amazonaws.util.StringUtils;

public class Utils {

    public static boolean isScalar(Shape shape) {
        // enums are treated as scalars in C2j.
        return !(isListShape(shape) || isStructure(shape) || isMapShape(shape));
    }

    public static boolean isStructure(Shape shape) {
        return shape.getType().equals("structure");
    }

    public static boolean isListShape(Shape shape) {
        return shape.getType().equals("list");
    }

    public static boolean isMapShape(Shape shape) {
        return shape.getType().equals("map");
    }

    public static boolean isEnumShape(Shape shape) {
        return shape.getEnumValues() != null;
    }

    public static boolean isExceptionShape(Shape shape) {
        return shape.isException() || shape.isFault();
    }

    public static String getAsyncInterfaceName(String interfaceNamePrefix) {
        return interfaceNamePrefix + Constants.ASYNC_SUFFIX;
    }

    public static String getClientName(String clientNamePrefix) {
        return clientNamePrefix + Constants.CLIENT_NAME_SUFFIX;
    }

    public static String unCapitialize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        return name.length() < 2 ? StringUtils.lowerCase(name) : StringUtils.lowerCase(name.substring(0, 1))
                 + name.substring(1);

    }

    public static String capitialize(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        return name.length() < 2 ? StringUtils.upperCase(name) : StringUtils.upperCase(name.substring(0, 1))
                + name.substring(1);

    }

    public static String getDefaultEndpointWithoutHttpProtocol(String endpoint) {

        if (endpoint == null) {
            return null;
        }
        if (endpoint.startsWith("http://")) {
            return endpoint.substring("http://".length());
        }
        if (endpoint.startsWith("https://")) {
            return endpoint.substring("https://".length());
        }
        return endpoint;
    }

    public static String getProtocolDefaultExceptionUnmarshaller(String protocol) {
        Protocol awsProtocol = Protocol.fromValue(protocol);
        switch (awsProtocol) {
        case EC2:
            return "LegacyErrorUnmarshaller";
        case QUERY:
        case REST_XML:
            return "StandardErrorUnmarshaller";
            // JSON protocols have the Error Unmarshaller hard coded in the templates. They are not
            // configurable like XML based protocols
        case JSON:
        case REST_JSON:
            return null;
        default:
            throw new RuntimeException("Unknown protocol " + protocol);
        }
    }

    public static File createDirectory(String path) {
        if (isNullOrEmpty(path)) {
            throw new IllegalArgumentException(
                    "Invalid path directory. Path directory cannot be null or empty");
        }

        final File dir = new File(path);
        createDirectory(dir);
        return dir;
    }

    public static void createDirectory(File dir) {
        if (!(dir.exists())) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Not able to create directory. "
                        + dir.getAbsolutePath());
            }
        }
    }

    public static File createFile(String dir, String fileName) throws IOException {

        if (isNullOrEmpty(fileName)) {
            throw new IllegalArgumentException(
                    "Invalid file name. File name cannot be null or empty");
        }

        createDirectory(dir);

        File file = new File(dir, fileName);

        if (!(file.exists())) {
            if (!(file.createNewFile())) {
                throw new RuntimeException("Not able to create file . "
                        + file.getAbsolutePath());
            }
        }

        return file;
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (Exception e) {
            LOGGER.debug("Not able to close the stream.");
        }
    }

    /**
     * Return an InputStream of the specified resource, failing if it can't be found.
     *
     * @param clzz
     * @param location
     *            Location of resource
     */
    public static InputStream getRequiredResourceAsStream(Class<?> clzz, String location) {
        InputStream resourceStream = clzz.getResourceAsStream(location);

        if (resourceStream == null) {
            // Try with a leading "/"
            if (!location.startsWith("/")) {
                resourceStream = clzz.getResourceAsStream("/" + location);
            }
            if (resourceStream == null) {
                throw new RuntimeException("Resource file was not found at location " + location);
            }
        }

        return resourceStream;
    }

    /**
     * Retrieve system property by name, failing if it's not found
     *
     * @param propertyName
     * @param errorMsgIfNotFound
     * @return Value of property if it exists
     */
    public static String getRequiredSystemProperty(String propertyName, String errorMsgIfNotFound) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue == null) {
            throw new RuntimeException(errorMsgIfNotFound);
        }
        return propertyValue;
    }

    /**
     * Retrieve optional system property by name, returning null if not found
     *
     * @param propertyName
     * @return Value of property if it exists, null if it does not
     */
    public static String getOptionalSystemProperty(String propertyName) {
        return System.getProperty(propertyName);
    }

    /**
     * Throws IllegalArgumentException with the specified error message if the input
     * is null, otherwise return the input as is.
     */
    public static <T> T assertNotNull(T argument, String msg) {
        if (argument == null) throw new IllegalArgumentException(msg);
        return argument;
    }

    /**
     * Search for intermediate shape model by its c2j name.
     *
     * @throws IllegalArgumentException
     *             if the specified c2j name is not found in the intermediate
     *             model.
     */
    public static ShapeModel findShapeModelByC2jName(
            IntermediateModel intermediateModel, String shapeC2jName) {

        for (ShapeModel shape : intermediateModel.getShapes().values()) {
            if (shape.getC2jName().equals(shapeC2jName)) {
                return shape;
            }
        }

        throw new IllegalArgumentException(shapeC2jName
                + " shape (c2j name) does not exist in the intermediate model.");
    }

    /**
     * Create the ShapeMarshaller to the input shape from the specified Operation.
     * The input shape in the operation could be empty.
     *
     * @param service
     * @param operation
     * @return
     */
    public static ShapeMarshaller createInputShapeMarshaller(ServiceMetadata service, Operation operation) {

        if (operation == null)
            throw new IllegalArgumentException(
                    "The operation parameter must be specified!");

        ShapeMarshaller marshaller = new ShapeMarshaller()
                .withAction(operation.getName())
                .withVerb(operation.getHttp().getMethod())
                .withRequestUri(operation.getHttp().getRequestUri());
        Input input = operation.getInput();
        if (input != null) {
            marshaller.setLocationName(input.getLocationName());
            // Pass the xmlNamespace trait from the input reference
            XmlNamespace xmlNamespace = input.getXmlNamespace();
            if (xmlNamespace != null) {
                marshaller.setXmlNameSpaceUri(xmlNamespace.getUri());
            }
        }
        if (!StringUtils.isNullOrEmpty(service.getTargetPrefix())) {
            marshaller.setTarget(service.getTargetPrefix() + "."
                    + operation.getName());
        }
        return marshaller;

    }
}