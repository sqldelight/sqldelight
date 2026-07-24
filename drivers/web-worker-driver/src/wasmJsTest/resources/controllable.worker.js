let failNextBegin = false;
let failNextEnd = false;
let failNextRollback = false;

function respond(id, results = { values: [] }) {
  self.postMessage({ id, results, rowCount: 0 });
}

function fail(id, message) {
  self.postMessage({ id, error: message });
}

self.onmessage = (event) => {
  const data = event.data;
  switch (data.action) {
    case "exec":
      switch (data.sql) {
        case "hold":
          return;
        case "worker-error":
          throw new Error("terminal worker failure");
        case "booleans":
          respond(data.id, { values: [[true, false, 1, 0]] });
          return;
        case "fail-next-begin":
          failNextBegin = true;
          break;
        case "fail-next-end":
          failNextEnd = true;
          break;
        case "fail-next-rollback":
          failNextRollback = true;
          break;
      }
      respond(data.id);
      return;
    case "begin_transaction":
      if (failNextBegin) {
        failNextBegin = false;
        fail(data.id, "begin failed");
      } else {
        respond(data.id);
      }
      return;
    case "end_transaction":
      if (failNextEnd) {
        failNextEnd = false;
        fail(data.id, "end failed");
      } else {
        respond(data.id);
      }
      return;
    case "rollback_transaction":
      if (failNextRollback) {
        failNextRollback = false;
        fail(data.id, "rollback failed");
      } else {
        respond(data.id);
      }
      return;
    case "close":
    case "delete_database":
      respond(data.id);
      return;
    default:
      fail(data.id, `Unsupported action: ${data.action}`);
  }
};
