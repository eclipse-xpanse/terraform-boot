/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.utils;

import org.eclipse.serializer.Serializer;
import org.eclipse.serializer.SerializerFoundation;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.springframework.stereotype.Component;

/** class to manage TerraformResult serialization and deserialization using eclipse-serializer. */
@Component
public class TerraformResultSerializer {

    /**
     * serialize TerraformResult object.
     *
     * @param result TerraformResult.
     * @return byte[].
     */
    public byte[] serialize(TerraformResult result) {
        final SerializerFoundation<?> foundation =
                SerializerFoundation.New().registerEntityTypes(TerraformResult.class);
        Serializer<byte[]> serializer = Serializer.Bytes(foundation);
        return serializer.serialize(result);
    }

    /**
     * deserialize TerraformResult object.
     *
     * @param data byte[].
     * @return TerraformResult.
     */
    public TerraformResult deserialize(byte[] data) {
        final SerializerFoundation<?> foundation =
                SerializerFoundation.New().registerEntityTypes(TerraformResult.class);
        Serializer<byte[]> serializer = Serializer.Bytes(foundation);
        return serializer.deserialize(data);
    }
}
