# LocalLLMChat

LocalLLMChat is a lightweight desktop chat application written in Java that lets you talk to a locally hosted LLM (such as LLaMA) through a simple graphical interface. It’s designed to be straightforward, fast, and easy to modify for experimentation and personal use.

This project was built as a hands-on learning tool for working with local language models, Java Swing UI, and API communication — while keeping everything offline and under your control.

---

## ✨ Features

- Simple desktop chat interface
- Connects to a local LLM backend over HTTP
- Conversation history handling
- Clean, minimal UI
- No cloud dependencies
- Fast startup and low overhead

---

## 🛠 Requirements

- Java 17 or newer (recommended)
- Maven
- A locally running LLM server (for example, a LLaMA server)

---

## 🚀 How to Run

1. Clone the repository:

2. Open the project in IntelliJ IDEA (or your preferred IDE)

3. Make sure Maven dependencies are downloaded

4. Run `Main.java`

Make sure your local LLM server is running before starting the application.

---

## 📁 Project Structure

src/
└── main/  
└── java/  
└── org/example/  
├── Main.java  
├── ChatWindow.java  
├── Conversation.java  
└── LlamaClient.java  
pom.xml  
.gitignore


---

## 🔒 Privacy

This application is designed to run completely locally. No data is sent to external servers unless you configure it to do so. All conversations stay on your machine.

---

## 📌 Notes

- This project is still under development and meant for personal use and learning.
- The code is intentionally kept easy to read and modify.
- Feel free to fork it, experiment with it, or build your own features on top.

---

## 📄 License

This project is open-source and available under the MIT License.

---

Developed by **Owen**
