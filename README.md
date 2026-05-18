
<h1 align="center">Scholar</h1>

<p align="center">
Offline AI Socratic Science Tutor — Gemma 4 + Unsloth + llama.cpp
</p>

<p align="center">
<a href="https://www.kaggle.com/competitions/gemma-4-good-hackathon/"><img src="https://img.shields.io/badge/Kaggle-Gemma%204%20Good%20Hackathon-blue?style=flat-square"</img></a>
<a href="https://github.com/rajasingh012/scholar-ai"><img src="https://img.shields.io/badge/Platform-Future%20of%20Education-green?style=flat-square"</img></a>
</p>

Scholar is a mobile app that brings an AI science tutor to students in areas with poor or no internet. Built for Class 6–8 NCERT Science, it runs entirely on-device — no server, no API, no internet required after model download.

## How It Works

```
NCERT PDFs ──► Dialogue Generator (MiniMax M2.7) ──► JSONL
                                               ──► Kaggle GPU (Unsloth QLoRA) ──► GGUF Q4_K_M
                                                                                          │
                                                                                          ▼
                                                                                   Android App (llama.cpp)
```

No server. No RAG. Works completely offline.

## Key Features

- **On-device inference** — Gemma 4 E2B runs locally via llama.cpp, no cloud needed
- **Socratic teaching** — tutor asks guiding questions so students discover answers themselves
- **Quick Revision** — structured explanations + test questions for exam prep
- **Fully offline** — after downloading the ~3.4 GB GGUF model, works without internet
- **Unsloth fine-tuning** — QLoRA on Kaggle T4 GPU (46 min training, 0.69 eval loss)

## Stack

| Layer | Technology |
|-------|-----------|
| Base Model | [unsloth/gemma-4-E2B-it-unsloth-bnb-4bit](https://huggingface.co/unsloth/gemma-4-E2B-it-unsloth-bnb-4bit) |
| Fine-tuning | Unsloth QLoRA (4-bit) on Kaggle T4 GPU |
| Quantization | GGUF Q4_K_M (~3.4 GB) |
| On-device inference | [llama.cpp](https://github.com/ggml-org/llama.cpp) |
| Mobile | Android (Kotlin + Jetpack Compose) |
| Fine-tuned model | [rajasingh012/vidya-gemma4-e2b-gguf](https://huggingface.co/rajasingh012/vidya-gemma4-e2b-gguf) |

## Training Data

- **649 Socratic dialogues** generated from NCERT Class 6–8 Science textbooks
- Dialogue types: Socratic (60%) + Quick Revision (40%)
- Generated using MiniMax M2.7 API from 111 NCERT PDF chapters
- Dataset: [rajasinghg/ncert-vidya-socratic-dialogues](https://www.kaggle.com/datasets/rajasinghg/ncert-vidya-socratic-dialogues)

## Kaggle Notebooks

| Kernel | Purpose |
|--------|---------|
| [vidya-fine-tune-gguf-gpu-gemma-4-e2b-qlora](https://www.kaggle.com/code/rajasinghg/vidya-fine-tune-gguf-gpu-gemma-4-e2b-qlora) | Training — fine-tune + GGUF export + HF upload |
| [vidya-infer-v2-gemma-4-e2b-lora-inference](https://www.kaggle.com/code/rajasinghg/vidya-infer-v2-gemma-4-e2b-lora-inference) | Inference — quality testing |

## Build Instructions

Prerequisites:
* Android Studio 2024.3.1+
* NDK 27.2.12479018
* CMake 3.31.6

```bash
git clone https://github.com/rajasingh012/scholar-ai.git
cd scholar-ai
git submodule update --init --recursive   # pull llama.cpp
```

Open in Android Studio: `File` > `Open` > select the cloned repo, then run on device or emulator.

## Kaggle Hackathon

Submitted to the **Future of Education** track of the [Gemma 4 Good Hackathon](https://www.kaggle.com/competitions/gemma-4-good-hackathon/).

## License

MIT License.

## Acknowledgments

Built on [llama.cpp](https://github.com/ggml-org/llama.cpp), [Unsloth](https://unsloth.ai/), and [Gemma 4 E2B](https://huggingface.co/unsloth/gemma-4-E2B-it-unsloth-bnb-4bit). Fine-tuning infrastructure powered by Kaggle.
