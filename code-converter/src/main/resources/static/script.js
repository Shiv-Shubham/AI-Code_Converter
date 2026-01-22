function convert() {
    const to = document.getElementById('to').value;
    const code = document.getElementById('code').value;

    const outputBox = document.getElementById("outputBox");
    const outputCode = document.getElementById("output");

    outputBox.style.display = "block";
    outputCode.textContent = "";
    const loading = document.getElementById("loadingOverlay");
    loading.style.display = "flex";

    const langMap = {
        "Java": "java",
        "C++": "cpp",
        "Python": "python",
        "JavaScript": "javascript"
    };

    const prismLang = langMap[to] || "cpp";

    outputCode.className = "";
    outputCode.classList.add("language-" + prismLang);

    const params = new URLSearchParams({
        targetLanguage: to,
        code: code
    });

    const eventSource = new EventSource("/api/convert/stream?" + params.toString());

  eventSource.onmessage = function (event) {
  loading.style.display = "none";

      let chunk = event.data;

      // Remove markdown fences if any
      chunk = chunk.replace(/```[a-zA-Z]*/g, "").replace(/```/g, "");

      // 🔥 Decode JSON escaped characters
      chunk = chunk
          .replace(/\\u003c/g, "<")
          .replace(/\\u003e/g, ">")
          .replace(/\\u0026/g, "&")
          .replace(/\\n/g, "\n")
          .replace(/\\t/g, "\t")
          .replace(/\\r/g, "\r");

      outputCode.textContent += chunk;
      Prism.highlightElement(outputCode);
  };


    eventSource.onerror = function () {
    loading.style.display = "none";
        eventSource.close();

    };
}
function clearInput() {
    document.getElementById("code").value = "";
     const outputCode = document.getElementById("output");
        outputCode.textContent = "";

}

function copyOutput() {
    const text = document.getElementById("output").textContent;
    if (!text.trim()) return;

    navigator.clipboard.writeText(text).then(() => {
        showToast("Copied to clipboard");
    });
}
function showToast(message) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.classList.add("show");

    setTimeout(() => {
        toast.classList.remove("show");
    }, 2000);
}

