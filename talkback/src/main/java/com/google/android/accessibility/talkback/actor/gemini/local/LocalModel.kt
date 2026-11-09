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
  /** The commit of [repo] to download from, so that a later upload can't change the file. */
  val revision: String,
  /** The name to keep the file under on the phone, when it is not just [id]. */
  private val localName: String? = null,
  val sizeBytes: Long,
  /** The size of the file this model had before [revision]. Those files stay usable. */
  val previousSizeBytes: Long? = null,
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
  /**
   * Whether the model keeps to the JSON format that Describe screen asks for. Others are asked for
   * plain sentences.
   */
  val writesJson: Boolean = false,
) {
  val localFileName: String
    get() = localName ?: "$id.litertlm"

  val minTotalRamBytes: Long
    get() = minTotalRamGib * GIB

  val url: String
    get() = "https://huggingface.co/$repo/resolve/$revision/$fileName"

  companion object {
    private const val GIB = 1024L * 1024L * 1024L

    val entries: List<LocalModel> = listOf(
    LocalModel(
      id = "e2b",
      name = "Gemma 4 E2B",
      repo = "litert-community/gemma-4-E2B-it-litert-lm",
      fileName = "gemma-4-E2B-it.litertlm",
      revision = "3d48132a00cf76de57d926de30f5c5c5ed9b8c0d",
      localName = "gemma-4-E2B-it.litertlm",
      sizeBytes = 2_588_147_712L,
      sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
      minTotalRamGib = 6,
      note = "Recommended. Best answers for its size.",
      maxTokens = 6144,
      writesJson = true,
    ),
    LocalModel(
      id = "e4b",
      name = "Gemma 4 E4B",
      repo = "litert-community/gemma-4-E4B-it-litert-lm",
      fileName = "gemma-4-E4B-it.litertlm",
      revision = "71064b01bd5e2bfd9a5f9dd52df86ae4800138c1",
      localName = "gemma-4-E4B-it.litertlm",
      sizeBytes = 3_659_530_240L,
      sha256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0",
      minTotalRamGib = 8,
      note = "Better answers, slower. Needs a phone with a lot of memory.",
      maxTokens = 6144,
      writesJson = true,
    ),
    LocalModel(
      id = "gemma4-12b",
      name = "Gemma 4 12B",
      repo = "litert-community/gemma-4-12B-it-litert-lm",
      fileName = "gemma-4-12B-it.litertlm",
      revision = "12c90aac44ff1b598f0404de0a9ce92dd97b5554",
      sizeBytes = 6_883_278_368L,
      sha256 = "58fd31b778ca2c21c80d634fb34fc5a89d11d563a38dfd3cbf1b40dbf252a8b6",
      minTotalRamGib = 16,
      note = "Very large. For phones with 16 GB of memory or more.",
      maxTokens = 6144,
      experimental = true,
      writesJson = true,
    ),
    LocalModel(
      id = "smolvlm2-500m",
      name = "SmolVLM2 500M",
      repo = "litert-community/SmolVLM2-500M",
      fileName = "SmolVLM2-500M.litertlm",
      revision = "0e794c26b4de1ff85a693653567e8d2e8e211dbf",
      sizeBytes = 360_857_600L,
      previousSizeBytes = 360_822_960L,
      sha256 = "f820b08de8ad1d43ae2c1002098e11ea2720348057dae00f7baee16f8c8b0d3d",
      minTotalRamGib = 3,
      note = "Tiny and fast. Short answers.",
      experimental = true,
    ),
    LocalModel(
      id = "smolvlm2-2b",
      name = "SmolVLM2 2.2B",
      repo = "litert-community/SmolVLM2-2.2B",
      fileName = "SmolVLM2-2.2B.litertlm",
      revision = "34c0ad91d6548a812fa0560f1b49d29fef0701de",
      sizeBytes = 1_511_161_856L,
      previousSizeBytes = 1_511_123_888L,
      sha256 = "5a64247a1e62dea0d96ad26075988d3a66d2f463e0e8de29e33008ee26a6cbcf",
      minTotalRamGib = 4,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "lfm25-vl-450m",
      name = "LFM2.5-VL 450M",
      repo = "litert-community/LFM2.5-VL-450M",
      fileName = "LFM2.5-VL-450M_int4_fixB.litertlm",
      revision = "d52b2efd0dcfe4332e20aa443572503332941cd7",
      sizeBytes = 406_896_640L,
      previousSizeBytes = 406_817_104L,
      sha256 = "18cb1f93cf35cc8a7c3ea47a9854bacecccf2e80cca574e49e6da1bbd940fcec",
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
      revision = "96905faa09e7138c972fe24dcf46e31b3e4d14da",
      sizeBytes = 1_298_235_392L,
      previousSizeBytes = 1_298_139_472L,
      sha256 = "30abec4f4bd2a22f3316acfc17028e819224ce3bdb876e8c7f33d25378e7dd56",
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
      revision = "ae62d1858a94adf81e42cb3ab2a9d66c230ea51c",
      sizeBytes = 2_352_152_576L,
      previousSizeBytes = 2_352_023_888L,
      sha256 = "08b108daea829b01a9ef405a64ec4c4a553373ad0b96f3d3bdebe8e1737504fb",
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
      revision = "d2ecfb5cceb4916329d59181f3f24e59e4f25a19",
      sizeBytes = 1_303_724_032L,
      previousSizeBytes = 1_302_262_448L,
      sha256 = "7413f51eafb82e7cf0b707fa506eddfc76c10433b26fa8f0dc2f051970d2feb5",
      minTotalRamGib = 4,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "qwen35-vl-2b",
      name = "Qwen3.5 2B (vision)",
      repo = "litert-community/Qwen3.5-2B",
      fileName = "Qwen3.5-2B-VL_int8.litertlm",
      revision = "f24013ba6844bd39be2d1f68e8d2421a77eed1f2",
      sizeBytes = 3_152_429_056L,
      previousSizeBytes = 3_148_905_040L,
      sha256 = "90ece8de37537b757ef21075a7d17c14bc11ac29793dada058a5be7c3fe03ccc",
      minTotalRamGib = 6,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "qwen2-vl-2b",
      name = "Qwen2-VL 2B",
      repo = "litert-community/Qwen2-VL-2B",
      fileName = "Qwen2-VL-2B.litertlm",
      revision = "65ef506f192be8d13aa77f26ca517863fd95da86",
      sizeBytes = 1_783_463_936L,
      previousSizeBytes = 1_783_424_544L,
      sha256 = "b6f2f7f8a2f969ee80d0687a5e76171fa92c940ef36d87f98f5df4513b827788",
      minTotalRamGib = 4,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl3-1b",
      name = "InternVL3 1B",
      repo = "litert-community/InternVL3-1B",
      fileName = "InternVL3-1B.litertlm",
      revision = "93b8324d7476573891feefd0d169bea1755adc5e",
      sizeBytes = 737_378_304L,
      previousSizeBytes = 737_314_160L,
      sha256 = "18650b2df325d7c0bf59461099f9587ca94eee9a13ab358984c58138fb1a69f7",
      minTotalRamGib = 3,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl3-2b",
      name = "InternVL3 2B",
      repo = "litert-community/InternVL3-2B",
      fileName = "InternVL3-2B.litertlm",
      revision = "ebcf8184a6efe444a6f578ced388d4aa1dc915fc",
      sizeBytes = 1_429_700_608L,
      previousSizeBytes = 1_429_640_560L,
      sha256 = "44c652d4aae876a1e06086fbb5e7fcc385005b5aa928b160238337478c5ffd3f",
      minTotalRamGib = 4,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl35-1b",
      name = "InternVL3.5 1B",
      repo = "litert-community/InternVL3_5-1B",
      fileName = "model.litertlm",
      revision = "3e48aed2c49acbd4ad0e09a1f882b77adf4e5861",
      sizeBytes = 817_545_216L,
      previousSizeBytes = 817_517_936L,
      sha256 = "7493d2c82fb3dffbc471c1dcecf97280faa71227c1700a1428473d33cd396957",
      minTotalRamGib = 3,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl35-2b",
      name = "InternVL3.5 2B",
      repo = "litert-community/InternVL3_5-2B",
      fileName = "model.litertlm",
      revision = "db81ac2aa206d11b04ab284c2e34a6f30aaa8d42",
      sizeBytes = 1_613_250_560L,
      previousSizeBytes = 1_613_223_280L,
      sha256 = "acc9a204d4c75ce4a586767421e446df7d9dc00e00115ac79714cbbe654a10c5",
      minTotalRamGib = 4,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "internvl35-4b",
      name = "InternVL3.5 4B",
      repo = "litert-community/InternVL3_5-4B",
      fileName = "model.litertlm",
      revision = "29313d1733a35d3ba38808f971cbbfea1beee47a",
      sizeBytes = 2_992_226_304L,
      previousSizeBytes = 2_992_182_640L,
      sha256 = "6a791513ee510a46a6d0d17bd0f459503c19792e3087b992633f69f1fe5740eb",
      minTotalRamGib = 6,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "llava-ov-0.5b",
      name = "LLaVA-OneVision 0.5B",
      repo = "litert-community/LLaVA-OneVision-0.5B",
      fileName = "LLaVA-OneVision-0.5B.litertlm",
      revision = "6716233899ecf7b2dd7d7570b1f931c568ba7090",
      sizeBytes = 828_669_952L,
      previousSizeBytes = 828_590_400L,
      sha256 = "5bf4a139cf9739cdc3968a0fde93cc49970dd20e533082fb66191872868f0ea8",
      minTotalRamGib = 3,
      note = "Small and fast.",
      experimental = true,
    ),
    LocalModel(
      id = "fastvlm-0.5b",
      name = "FastVLM 0.5B",
      repo = "litert-community/FastVLM-0.5B",
      fileName = "FastVLM-0.5B.litertlm",
      revision = "460013246392191f8532e45e518576bb6513eace",
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
      revision = "b5c78695a3334f579309556deb9edde03ef13804",
      sizeBytes = 2_148_743_168L,
      previousSizeBytes = 2_148_712_880L,
      sha256 = "4a705046aa62fa7328340b99835c3a44424d2dc53db511c5e50d8d9dec6a1db4",
      minTotalRamGib = 6,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "north-micro",
      name = "North Micro Vision",
      repo = "litert-community/North-Micro-Vision-Instruct",
      fileName = "North-Micro-Vision-Instruct_int4.litertlm",
      revision = "d20478ae2d5387146730ac998e21e36ec67abd1e",
      sizeBytes = 2_190_606_336L,
      previousSizeBytes = 2_190_562_720L,
      sha256 = "cf50d07daf2173b42f341ea8d2e49847e6585177860ad59f21d1adeadf9c74f2",
      minTotalRamGib = 6,
      note = "Medium size.",
      experimental = true,
    ),
    LocalModel(
      id = "mage-vl",
      name = "Mage-VL",
      repo = "litert-community/Mage-VL",
      fileName = "Mage-VL.litertlm",
      revision = "605dd5759315392459df844097f493d5f64670aa",
      sizeBytes = 2_812_051_456L,
      previousSizeBytes = 2_811_977_104L,
      sha256 = "aecbf022d09ab75a66cdbe91f36d696704d7a1cfac226fb6bcd4239fb32b7f24",
      minTotalRamGib = 6,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "decider-2b",
      name = "Decider 2B Vision",
      repo = "litert-community/decider-2b-vision-LiteRT",
      fileName = "decider-2b-vision_int8.litertlm",
      revision = "5cbe1e2c279852cc39c1165017e53d81ee6a4520",
      sizeBytes = 3_171_155_968L,
      previousSizeBytes = 3_171_081_088L,
      sha256 = "fa2d3dfa10356ac4f79fcb3cd2a3237eb1f767b810d3a3a0939206d18d9ac7ac",
      minTotalRamGib = 6,
      note = "Larger and slower.",
      experimental = true,
    ),
    LocalModel(
      id = "minicpm-v4",
      name = "MiniCPM-V 4",
      repo = "litert-community/MiniCPM-V-4",
      fileName = "MiniCPM-V-4-int8.litertlm",
      revision = "2d05ab8722280bd1f5e654c6a1a5e39c0e2bfc8c",
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
      revision = "3db397f5677217a3fcef9d99f43055d8aa90e3a8",
      sizeBytes = 1_390_313_472L,
      previousSizeBytes = 1_390_305_472L,
      sha256 = "39fb2d2c335217a71434962a00f4122414243fd8352d0a6c31a63cefbc18904b",
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
      revision = "3d48132a00cf76de57d926de30f5c5c5ed9b8c0d",
      sizeBytes = 3_206_299_658L,
      previousSizeBytes = 3_113_545_589L,
      sha256 = "72d056066f34362adcecf6b76995de67ad899f6f33dfe9079593c1acd2984191",
      minTotalRamGib = 8,
      note = "Gemma 4 E2B built for the NPU of Google Tensor G5.",
      experimental = true,
      npuChip = Accelerators.TENSOR_G5,
      writesJson = true,
    ),
    LocalModel(
      id = "e2b-tensor-g6",
      name = "Gemma 4 E2B (Tensor G6 NPU)",
      repo = "litert-community/gemma-4-E2B-it-litert-lm",
      fileName = "gemma-4-E2B-it_Google_Tensor_G6.litertlm",
      revision = "3d48132a00cf76de57d926de30f5c5c5ed9b8c0d",
      sizeBytes = 3_453_960_202L,
      previousSizeBytes = 3_313_938_293L,
      sha256 = "d6cbe8d625f4965a8c32565f65ae774d19598d0d967a4c2101a7e04988daaf58",
      minTotalRamGib = 8,
      note = "Gemma 4 E2B built for the NPU of Google Tensor G6.",
      experimental = true,
      npuChip = Accelerators.TENSOR_G6,
      writesJson = true,
    ),
    LocalModel(
      id = "fastvlm-0.5b-sm8750",
      name = "FastVLM 0.5B (Snapdragon 8 Elite NPU)",
      repo = "litert-community/FastVLM-0.5B",
      fileName = "FastVLM-0.5B.qualcomm.sm8750.litertlm",
      revision = "460013246392191f8532e45e518576bb6513eace",
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
      revision = "460013246392191f8532e45e518576bb6513eace",
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
