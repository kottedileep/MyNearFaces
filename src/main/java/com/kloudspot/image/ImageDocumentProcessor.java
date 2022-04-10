// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.kloudspot.image;

import com.google.inject.Inject;
import com.yahoo.component.chain.Chain;
import com.yahoo.docproc.DocumentProcessor;
import com.yahoo.docproc.Processing;
import com.yahoo.document.DataType;
import com.yahoo.document.Document;
import com.yahoo.document.DocumentOperation;
import com.yahoo.document.DocumentPut;
import com.yahoo.document.datatypes.*;
import com.yahoo.tensor.Tensor;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class ImageDocumentProcessor extends DocumentProcessor {

    private static final Logger logger = Logger.getLogger(ImageDocumentProcessor.class.getName());

    private static final String IMAGE_DOCUMENT_TYPE  = "images";

    @Override
    public Progress process(Processing processing) {
        for (DocumentOperation op : processing.getDocumentOperations()) {
            if(op instanceof DocumentPut) {

                logger.info("Document Processing");
                DocumentPut put = (DocumentPut) op;
                Document doc = put.getDocument();
                if (!doc.getDataType().isA(IMAGE_DOCUMENT_TYPE)) {
                    processing.getDocumentOperations().clear();
                    return Progress.DONE;
                }
                TensorFieldValue vector = (TensorFieldValue) doc.getFieldValue("image_vector");
                Optional<Tensor> tensorMaybe = vector.getTensor();
                if(tensorMaybe.isEmpty()) {
                    return Progress.PERMANENT_FAILURE.withReason("No 'image_vector' tensor field in document");
                }
            }
        }
        return Progress.DONE;
    }
}
