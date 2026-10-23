/*
 * Copyright 2026 Backtalk contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.actor.gemini.local

import com.google.android.accessibility.talkback.acceleration.Accelerators

/**
 * A vision model that Backtalk can run on the phone. The files come from the LiteRT community on
 * Hugging Face, and Backtalk checks each download against the hash recorded here.
 *
 * Only Gemma 4 E2B and E4B have been tried by the Backtalk developers. The others are community
 * conversions that are listed so that people can try them, and are marked [experimental].
 */
data class LocalModel(
  val id: String,
  val name: String,
  val repo: String,
  /** The name of the file in [repo]. */
  val fileName: String,
  /** The name to keep the file under on the phone, when it is not just [id]. */
  private val localName: String? = null,
  val sizeBytes: Long,
  val sha256: String,
  /** About how much total RAM a phone needs to run the model comfortably. */
  val minTotalRamGib: Int,
  val note: String,
  /** The longest prompt plus answer, or null to use what the model file says. */
  val maxTokens: Int? = null,
  val license: String = "Apache-2.0",
  val experimental: Boolean = false,
  /** The chip whose NPU the model is built for, or null for a model that runs on CPU and GPU. */
  val npuChip: String? = null,
) {
  val localFileName: String
    get() = localName ?: "$id.litertlm"

  val minTotalRamBytes: Long
    get() = minTotalRamGib * GIB

  val url: String
    get() = "https://huggingface.co/$repo/resolve/main/$fileName"

  companion object {
    private const val GIB = 1024L * 1024L * 1024L

    val entries: List<LocalModel> = listOf(
    LocalModel(
      id = "e2b",
      name = "Gemma 4 E2B",
      repo = "litert-community/gemma-4-E2B-it-litert-lm",
      fileName = "gemma-4-E2B-it.litertlm",
      localName = "gemma-4-E2B-it.litertlm",
      sizeBytes = 2_588_147_712L,
      sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
      minTotalRamGib = 6,
      note = "Recommended. Best answers for its size.",
      maxTokens = 6144,
    ),
    LocalModel(
      id = "e4b",
      name = "Gemma 4 E4B",
      repo = "litert-community/gemma-4-E4B-it-litert-lm",
      fileName = "gemma-4-E4B-it.litertlm",
      localName = "gemma-4-E4B-it.litertlm",
      sizeBytes = 3_659_530_240L,
      sha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
      minTotalRamGib = 8,
      note = "Better answers, slower. Needs a phone with a lot of memory.",
      maxTokens = 6144,
    ),
    LocalModel(
      id = "gemma4-12b",
      name = "Gemma 4 12B",
      repo = "litert-community/gemma-4-12B-it-litert-lm",
      fileName = "gemma-4-12B-it.litertlm",
      sizeBytes = 6_883_278_368L,
      sha256 = "58fd31b778ca2c21c80d634fb34fc5a89d11d563a38dfd3cbf1b40dbf252a8b6",
      minTotalRamGib = 16,
      note = "Very large. For phones with 16 GB of memory or more.",
      maxTokens = 6144,
      experimental = true,
    ),
    LocalModel(
      id = "smolvlm2-500m",
      name = "SmolVLM2 500M",
      repo = "litert-community/SmolVLM2-500M",
      fileName = "SmolVLM2-500M.litertlm",
      sizeBytes = 360_822_960L,
      sha256 = "b808b328d845a600a33c5295f93d9217487317bd334dbc91b2a8d50e26e60ad0",
      minTotalRamGib = 3,
      note = "Tiny and fast. Short answers.",
      experimental = true,
    ),
    LocalModel(
      id = "smolvlm2-2b",
      name = "SmolVLM2 2.2B",
      repo = "litert-community/SmolVLM2-2.2B",
      fileName = "SmolVLM2-2.2B.litertlm",
      sizeBytes = 1_511_123_888L,
      sha256 = "fab0ba947699ab405ff8fb9ea2de6c9af3cdfb598ea93f57631a268f3d631da2",
      minTotalRamGib = 4,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "lfm25-vl-450m",
      name = "LFM2.5-VL 450M",
      repo = "litert-community/LFM2.5-VL-450M",
      fileName = "LFM2.5-VL-450M_int4_fixB.litertlm",
      sizeBytes = 406_817_104L,
      sha256 = "6854cd9677a34e680070b60076793e49aeb3224de7cb4101076b261cb9bf6033",
      minTotalRamGib = 3,
      note = "Tiny and fast.",
      license = "Custom licence",
      experimental = true,
    ),
    LocalModel(
      id = "lfm25-vl-1.6b",
      name = "LFM2.5-VL 1.6B",
      repo = "litert-community/LFM2.5-VL-1.6B",
      fileName = "LFM2.5-VL-1.6B_int4_fixB.litertlm",
      sizeBytes = 1_298_139_472L,
      sha256 = "79ca9db8af91e40486a5dfcaf26549180fd6c811ad85f826e3aef20ad0bd565c",
      minTotalRamGib = 4,
      note = "Small and fast.",
      license = "Custom licence",
      experimental = true,
    ),
    LocalModel(
      id = "lfm25-vl-3b",
      name = "LFM2.5-VL 3B",
      repo = "litert-community/LFM2.5-VL-3B",
      fileName = "LFM2.5-VL-3B_int4_fixB.litertlm",
      sizeBytes = 2_352_023_888L,
      sha256 = "2393cfba9d78e930bca56e92a752b222bc8b0cb111356faf6c5f23d021a3fa5e",
      minTotalRamGib = 6,
      note = "Medium size.",
      license = "Custom licence",
      experimental = true,
    ),
    LocalModel(
      id = "qwen35-vl-0.8b",
      name = "Qwen3.5 0.8B (vision)",
      repo = "litert-community/Qwen3.5-0.8B",
      fileName = "Qwen3.5-0.8B-VL_int8.litertlm",
      sizeBytes = 1_302_262_448L,
      sha256 = "e3360b658c929ff35ab740a21f5e4b688096a72e351b8d347e26ccda314121b7",
      minTotalRamGib = 4,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "qwen35-vl-2b",
      name = "Qwen3.5 2B (vision)",
      repo = "litert-community/Qwen3.5-2B",
      fileName = "Qwen3.5-2B-VL_int8.litertlm",
      sizeBytes = 3_148_905_040L,
      sha256 = "f8821e403dcfc939a033f6b2c9633a5d6b78c29a380ca825e0b90a62df61afe5",
      minTotalRamGib = 6,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "qwen2-vl-2b",
      name = "Qwen2-VL 2B",
      repo = "litert-community/Qwen2-VL-2B",
      fileName = "Qwen2-VL-2B.litertlm",
      sizeBytes = 1_783_424_544L,
      sha256 = "cf481776bcb16fe539a38c924a67fc213bb5ee55e6fa600729f060e64849066b",
      minTotalRamGib = 4,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl3-1b",
      name = "InternVL3 1B",
      repo = "litert-community/InternVL3-1B",
      fileName = "InternVL3-1B.litertlm",
      sizeBytes = 737_314_160L,
      sha256 = "7cf87c35cf364d04bd2e6f957e3a0366830ad0b46eca3dd81e0000891fd1284a",
      minTotalRamGib = 3,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl3-2b",
      name = "InternVL3 2B",
      repo = "litert-community/InternVL3-2B",
      fileName = "InternVL3-2B.litertlm",
      sizeBytes = 1_429_640_560L,
      sha256 = "cb7d63cbf2f5d9b3eb307012b54dc4fc4d68fef78dc3a5d4fbba41c59812f9b6",
      minTotalRamGib = 4,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl35-1b",
      name = "InternVL3.5 1B",
      repo = "litert-community/InternVL3_5-1B",
      fileName = "model.litertlm",
      sizeBytes = 817_517_936L,
      sha256 = "df984928897c84aa2d6603651850a809dc1fbb2ec5f42c048b988b2d1bd1d456",
      minTotalRamGib = 3,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl35-2b",
      name = "InternVL3.5 2B",
      repo = "litert-community/InternVL3_5-2B",
      fileName = "model.litertlm",
      sizeBytes = 1_613_223_280L,
      sha256 = "0f288a1cca1674ca3c0c0ee1705aefd78e9bf163594d00f012c9a7817c3679e1",
      minTotalRamGib = 4,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl35-4b",
      name = "InternVL3.5 4B",
      repo = "litert-community/InternVL3_5-4B",
      fileName = "model.litertlm",
      sizeBytes = 2_992_182_640L,
      sha256 = "2721a08faaa12dfb3af5f336c76b56274093468c1e25c1b35edfddfdcc8cb649",
      minTotalRamGib = 6,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "llava-ov-0.5b",
      name = "LLaVA-OneVision 0.5B",
      repo = "litert-community/LLaVA-OneVision-0.5B",
      fileName = "LLaVA-OneVision-0.5B.litertlm",
      sizeBytes = 828_590_400L,
      sha256 = "7f7cd7ae3d2ae435a1f69651de02a9d51e916be60fdfef5cadc721725310971a",
      minTotalRamGib = 3,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "fastvlm-0.5b",
      name = "FastVLM 0.5B",
      repo = "litert-community/FastVLM-0.5B",
      fileName = "FastVLM-0.5B.litertlm",
      sizeBytes = 1_156_342_768L,
      sha256 = "ccba1e8bfa0bab78345f5d009fdffd20bd8c907cf39b4bc632e391b0a96f3b18",
      minTotalRamGib = 4,
      note = "Small and fast. Apple research licence.",
      license = "Apple research licence",
      experimental = true,
    ),
    LocalModel(
      id = "ovis25-2b",
      name = "Ovis2.5 2B",
      repo = "litert-community/Ovis2.5-2B",
      fileName = "Ovis2.5-2B.litertlm",
      sizeBytes = 2_148_712_880L,
      sha256 = "b8c5cb82e2a8b945e8f2b72576745acf22e61393074b345a2575245d21ab8e4b",
      minTotalRamGib = 6,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "north-micro",
      name = "North Micro Vision",
      repo = "litert-community/North-Micro-Vision-Instruct",
      fileName = "North-Micro-Vision-Instruct_int4.litertlm",
      sizeBytes = 2_190_562_720L,
      sha256 = "098c7f7a3d20985136d7cc7311a2477943e5d00a51a6d8d1042cd903d48a9e45",
      minTotalRamGib = 6,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "mage-vl",
      name = "Mage-VL",
      repo = "litert-community/Mage-VL",
      fileName = "Mage-VL.litertlm",
      sizeBytes = 2_811_977_104L,
      sha256 = "4231ea9f88ef2bd3aca92fd4554531f17717225418c4527ac858a8a9184dad72",
      minTotalRamGib = 6,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "decider-2b",
      name = "Decider 2B Vision",
      repo = "litert-community/decider-2b-vision-LiteRT",
      fileName = "decider-2b-vision_int8.litertlm",
      sizeBytes = 3_171_081_088L,
      sha256 = "5fb2e19aa2066d2e3955431366bc572edb7abbc6037db53d4eaf4a4a4d0e7bd9",
      minTotalRamGib = 6,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "minicpm-v4",
      name = "MiniCPM-V 4",
      repo = "litert-community/MiniCPM-V-4",
      fileName = "MiniCPM-V-4-int8.litertlm",
      sizeBytes = 4_214_021_104L,
      sha256 = "d45ef0cd8141597d731d002b72a152ab1a0f8b9cda8d9a0f5e1bc3a4689dcbd0",
      minTotalRamGib = 8,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "paddleocr-vl",
      name = "PaddleOCR-VL 1.6",
      repo = "litert-community/PaddleOCR-VL-1.6",
      fileName = "PaddleOCR-VL-1.6.litertlm",
      sizeBytes = 1_390_305_472L,
      sha256 = "314df8b34d6911990d488e825602550a5bbcc5d6700cb1a8914e853e9043f4f1",
      minTotalRamGib = 4,
      note = "Made for reading text in images, not for describing them.",
      experimental = true,
    ),
    // Built for the NPU of one chip each. Used, instead of the model chosen, when the processor is
    // the NPU on a phone with that chip.
    LocalModel(
      id = "e2b-tensor-g5",
      name = "Gemma 4 E2B (Tensor G5 NPU)",
      repo = "litert-community/gemma-4-E2B-it-litert-lm",
      fileName = "gemma-4-E2B-it_Google_Tensor_G5.litertlm",
      sizeBytes = 3_113_545_589L,
      sha256 = "af1082986639ecde7db95d91be6fe54f8b6b458104734c5bafc204e69d6852dc",
      minTotalRamGib = 8,
      note = "Gemma 4 E2B built for the NPU of Google Tensor G5.",
      experimental = true,
      npuChip = Accelerators.TENSOR_G5,
    ),
    LocalModel(
      id = "e2b-tensor-g6",
      name = "Gemma 4 E2B (Tensor G6 NPU)",
      repo = "litert-community/gemma-4-E2B-it-litert-lm",
      fileName = "gemma-4-E2B-it_Google_Tensor_G6.litertlm",
      sizeBytes = 3_313_938_293L,
      sha256 = "f86c7c19c736e9307267946edef58b0a45494a9127036d54d90dd6b7617c95b7",
      minTotalRamGib = 8,
      note = "Gemma 4 E2B built for the NPU of Google Tensor G6.",
      experimental = true,
      npuChip = Accelerators.TENSOR_G6,
    ),
    LocalModel(
      id = "fastvlm-0.5b-sm8750",
      name = "FastVLM 0.5B (Snapdragon 8 Elite NPU)",
      repo = "litert-community/FastVLM-0.5B",
      fileName = "FastVLM-0.5B.qualcomm.sm8750.litertlm",
      sizeBytes = 943_226_880L,
      sha256 = "bb3625f0d887927b01a6f8a3444580514b6165e09a8eb1af276cc14ed22d783e",
      minTotalRamGib = 4,
      note = "FastVLM 0.5B built for the NPU of Snapdragon 8 Elite. Apple research licence.",
      license = "Apple research licence",
      experimental = true,
      npuChip = Accelerators.SNAPDRAGON_8_ELITE,
    ),
    LocalModel(
      id = "fastvlm-0.5b-sm8850",
      name = "FastVLM 0.5B (Snapdragon 8 Elite Gen 5 NPU)",
      repo = "litert-community/FastVLM-0.5B",
      fileName = "FastVLM-0.5B.qualcomm.sm8850.litertlm",
      sizeBytes = 942_374_912L,
      sha256 = "0eb6860d5c3800dc02c86515a54387f0d8289e5096db291ff6e4a993e312b482",
      minTotalRamGib = 4,
      note = "FastVLM 0.5B built for the NPU of Snapdragon 8 Elite Gen 5. Apple research licence.",
      license = "Apple research licence",
      experimental = true,
      npuChip = Accelerators.SNAPDRAGON_8_ELITE_GEN_5,
    ),
    )

    /** The models the user can choose: those that run on the CPU and GPU. */
    val choices: List<LocalModel>
      get() = entries.filter { it.npuChip == null }

    val E2B: LocalModel = entries.first { it.id == "e2b" }
    val E4B: LocalModel = entries.first { it.id == "e4b" }

    fun fromId(id: String?): LocalModel? = entries.firstOrNull { it.id == id }
  }
}
