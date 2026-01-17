async function initialize() {
}

function handleMessage() {
  postMessage({
    id: this.data.id,
    results: { garbage: true },
  });
}

function handleError(err) {
  return postMessage({
    id: this.data.id,
    error: err,
  });
}

if (typeof importScripts === "function") {
  const ready = initialize();

  self.onmessage = (event) => {
    ready
      .then(handleMessage.bind(event))
      .catch(handleError.bind(event));
  }
}
