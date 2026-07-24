import sqlite3InitModule from "@sqlite.org/sqlite-wasm";

// The OPFS file is locked by whichever connection is writing, so a second tab's write fails with
// SQLITE_BUSY unless SQLite is told to retry. Applications can override this with a PRAGMA.
const BUSY_TIMEOUT_MILLIS = 5000;

let sqlite3 = null;
let database = null;
let databasePath = null;
let requestQueue = Promise.resolve();

function emptyResults() {
  return { values: [] };
}

function normalizeDatabasePath(databaseName) {
  if (typeof databaseName !== "string" || databaseName.trim().length === 0) {
    throw new Error("configure: databaseName must be a non-empty string");
  }

  const pathSegments = [];
  for (const segment of databaseName.trim().split("/")) {
    if (segment.length === 0 || segment === ".") {
      continue;
    }
    if (segment === "..") {
      if (pathSegments.length === 0) {
        throw new Error("configure: databaseName must stay within the OPFS root");
      }
      pathSegments.pop();
    } else {
      pathSegments.push(segment);
    }
  }

  if (pathSegments.length === 0) {
    throw new Error("configure: databaseName must identify a database file");
  }
  return `/${pathSegments.join("/")}`;
}

function requireConfigured(action) {
  if (database === null) {
    throw new Error(`${action}: Worker must be configured before use`);
  }
}

function requireStandardOpfs() {
  if (self.crossOriginIsolated !== true) {
    throw new Error(
      "configure: SQLite Wasm OPFS requires cross-origin isolation (COOP same-origin and COEP require-corp)",
    );
  }
  if (typeof SharedArrayBuffer === "undefined") {
    throw new Error("configure: SQLite Wasm OPFS requires SharedArrayBuffer");
  }
  if (
    typeof navigator === "undefined" ||
    navigator.storage === undefined ||
    typeof navigator.storage.getDirectory !== "function"
  ) {
    throw new Error("configure: Origin Private File System is unavailable");
  }
}

async function configure(data) {
  if (database !== null) {
    throw new Error("configure: Worker is already configured");
  }

  requireStandardOpfs();
  const path = normalizeDatabasePath(data.databaseName);
  const initializedSqlite3 = await sqlite3InitModule();
  if (
    initializedSqlite3.oo1 === undefined ||
    typeof initializedSqlite3.oo1.OpfsDb !== "function"
  ) {
    throw new Error("configure: SQLite Wasm standard OPFS support is unavailable");
  }

  const openedDatabase = new initializedSqlite3.oo1.OpfsDb(path, "cw");
  openedDatabase.exec(`PRAGMA busy_timeout = ${BUSY_TIMEOUT_MILLIS};`);
  sqlite3 = initializedSqlite3;
  database = openedDatabase;
  databasePath = path;
}

function execute(data) {
  requireConfigured("exec");
  if (typeof data.sql !== "string" || data.sql.length === 0) {
    throw new Error("exec: Missing query string");
  }
  if (data.params != null && !Array.isArray(data.params)) {
    throw new Error("exec: params must be an array");
  }

  const options = {
    sql: data.sql,
    rowMode: "array",
    returnValue: "resultRows",
  };
  if (data.params != null) {
    options.bind = data.params;
  }

  const totalChangesBefore = database.changes(true);
  const values = database.exec(options);
  return {
    results: { values },
    rowCount: database.changes(true) - totalChangesBefore,
  };
}

function executeTransaction(action, sql) {
  requireConfigured(action);
  database.exec(sql);
  return emptyResults();
}

function closeDatabase() {
  if (database !== null) {
    database.close();
  }
  database = null;
  databasePath = null;
}

async function deleteDatabase() {
  requireConfigured("delete_database");
  const path = databasePath;
  closeDatabase();

  const segments = path.split("/").filter(Boolean);
  const fileName = segments.pop();
  let directory = await navigator.storage.getDirectory();
  for (const segment of segments) {
    directory = await directory.getDirectoryHandle(segment);
  }
  await directory.removeEntry(fileName);
}

async function handleRequest(data) {
  switch (data && data.action) {
    case "configure":
      await configure(data);
      return { results: emptyResults(), closeWorker: false };
    case "exec":
      return { ...execute(data), closeWorker: false };
    case "begin_transaction":
      return {
        results: executeTransaction("begin_transaction", "BEGIN TRANSACTION;"),
        closeWorker: false,
      };
    case "end_transaction":
      return {
        results: executeTransaction("end_transaction", "END TRANSACTION;"),
        closeWorker: false,
      };
    case "rollback_transaction":
      return {
        results: executeTransaction("rollback_transaction", "ROLLBACK TRANSACTION;"),
        closeWorker: false,
      };
    case "delete_database":
      await deleteDatabase();
      return { results: emptyResults(), closeWorker: false };
    case "close":
      closeDatabase();
      sqlite3 = null;
      return { results: emptyResults(), closeWorker: true };
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

function errorMessage(error) {
  if (error instanceof Error && error.message.length > 0) {
    return error.message;
  }
  if (typeof error === "string" && error.length > 0) {
    return error;
  }
  return "SQLite Wasm worker request failed";
}

async function processRequest(data) {
  const id = data && data.id;
  try {
    const response = await handleRequest(data);
    self.postMessage({
      id,
      results: response.results,
      rowCount: response.rowCount,
    });
    if (response.closeWorker) {
      self.close();
    }
  } catch (error) {
    self.postMessage({ id, error: errorMessage(error) });
  }
}

self.onmessage = (event) => {
  requestQueue = requestQueue.then(() => processRequest(event.data));
};
