/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (4.2.3).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.test.realtrans.api;

import com.test.realtrans.model.ErrorModel;
import java.util.UUID;
import io.swagger.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2021-03-07T16:37:01.296+05:30[Asia/Calcutta]")

@Validated
@Api(value = "realTimeTranscription", description = "the realTimeTranscription API")
public interface RealTimeTranscriptionApi {

    /**
     * POST /realTimeTranscription : Perform Real Time Transcription
     * Real Time Transcription
     *
     * @param accept The requested content type for the response such as: application/xml , text/xml , application/json, text/javascript (for JSONP) Per the HTTP guidelines, this is just a hint and responses MAY have a different content type, such as a blob fetch where a successful response will just be the blob stream as the payload. (required)
     * @param interactionID Unique identifier for this interaction (optional)
     * @param applicationID Unique identifier that tracks the request across applications. (optional)
     * @param body  (optional)
     * @return OK (status code 200)
     *         or Bad Input (status code 400)
     *         or Cannot access directory (status code 403)
     *         or Unexpected Error (status code 200)
     */
    @ApiOperation(value = "Perform Real Time Transcription", nickname = "realTimeTranscriptionPost", notes = "Real Time Transcription", response = String.class, tags={ "Real Time Transcription", })
    @ApiResponses(value = { 
        @ApiResponse(code = 200, message = "OK", response = String.class),
        @ApiResponse(code = 400, message = "Bad Input", response = ErrorModel.class),
        @ApiResponse(code = 403, message = "Cannot access directory", response = ErrorModel.class),
        @ApiResponse(code = 200, message = "Unexpected Error", response = ErrorModel.class) })
    @RequestMapping(value = "/realTimeTranscription",
        produces = "application/json", 
        consumes = "application/json",
        method = RequestMethod.POST)
    ResponseEntity<String> realTimeTranscriptionPost(@ApiParam(value = "The requested content type for the response such as: application/xml , text/xml , application/json, text/javascript (for JSONP) Per the HTTP guidelines, this is just a hint and responses MAY have a different content type, such as a blob fetch where a successful response will just be the blob stream as the payload." ,required=true) @RequestHeader(value="Accept", required=true) String accept,@ApiParam(value = "Unique identifier for this interaction" ) @RequestHeader(value="Interaction-ID", required=false) Optional<UUID> interactionID,@ApiParam(value = "Unique identifier that tracks the request across applications." ) @RequestHeader(value="Application-ID", required=false) Optional<String> applicationID,@ApiParam(value = ""  )  @Valid @RequestBody(required = false) String body);

}
