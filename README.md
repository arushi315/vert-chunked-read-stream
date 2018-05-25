- To use: 

        ChunkedReadStream chunkedReadStream = new ChunkedReadStream((io.vertx.core.buffer.Buffer) emailResponse.getDelegate(),
                        1024, "transactionId");

        HttpServerResponse response = context.response().setChunked(true);

        Pump pump = Pump.pump(chunkedReadStream, response.getDelegate());

        chunkedReadStream.endHandler(event -> {
            LOGGER.debug("Successfully written buffer stream data into response for sent {}.",
                    rxChunkedReadStream.sent);
            if (response.ended() || response.closed()) {
                LOGGER.info("Response is already ended {} or closed {}", response.ended(), response.closed());
            } else {
                response.end();
            }
        });

        chunkedReadStream.exceptionHandler(ex -> {
            LOGGER.error("Error occurred while sending chunked buffer.  Error message {} :: {}",
                 ex.getClass(), ex.getMessage());
        });

        pump.start();