/// <definition types="es4x" />
// @ts-check

import * as API from '@vertx/core';

vertx
  .createHttpServer()
  .requestHandler(function (req) {
    req.response().end("Hello ES4X!");
  })
  .listen(3000);

console.log('Server started on port 3000');
