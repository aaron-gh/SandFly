/*
 * Copyright 2026 SandFly contributors
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

#include <errno.h>
#include <jni.h>
#include <linux/input.h>
#include <linux/uinput.h>
#include <stdbool.h>
#include <stdint.h>
#include <sys/ioctl.h>
#include <time.h>
#include <unistd.h>

#define JNI_NAME(name) Java_com_google_android_accessibility_talkback_shizuku_VolumeKeyDevice_##name
#define BITS_PER_WORD (8 * sizeof(unsigned long))
#define BIT_WORDS(count) (((count) + BITS_PER_WORD - 1) / BITS_PER_WORD)

static bool has_bit(const unsigned long *bits, int bit) {
    return (bits[bit / BITS_PER_WORD] & (1UL << (bit % BITS_PER_WORD))) != 0;
}

static jint volume_mask(const unsigned long *bits) {
    return (has_bit(bits, KEY_VOLUMEUP) ? 1 : 0) | (has_bit(bits, KEY_VOLUMEDOWN) ? 2 : 0);
}

JNIEXPORT jint JNICALL JNI_NAME(supportedKeys)(JNIEnv *env, jobject self, jint fd) {
    (void)env;
    (void)self;
    struct input_id id;
    unsigned long types[BIT_WORDS(EV_CNT)] = {0};
    unsigned long keys[BIT_WORDS(KEY_CNT)] = {0};
    if (ioctl(fd, EVIOCGID, &id) < 0 || (id.bustype != BUS_HOST && id.bustype != 0) ||
        ioctl(fd, EVIOCGBIT(0, sizeof(types)), types) < 0 ||
        !has_bit(types, EV_KEY) || has_bit(types, EV_REL) || has_bit(types, EV_ABS) ||
        has_bit(types, EV_SW) || ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keys)), keys) < 0) {
        return 0;
    }
    // Never take a device that also carries touch, power, or other keys.
    for (int key = 0; key < KEY_CNT; key++) {
        if (key != KEY_VOLUMEUP && key != KEY_VOLUMEDOWN && has_bit(keys, key)) return 0;
    }
    int clock = CLOCK_MONOTONIC;
    return ioctl(fd, EVIOCSCLOCKID, &clock) < 0 ? 0 : volume_mask(keys);
}

JNIEXPORT jint JNICALL JNI_NAME(keysDown)(JNIEnv *env, jobject self, jint fd) {
    (void)env;
    (void)self;
    unsigned long keys[BIT_WORDS(KEY_CNT)] = {0};
    return ioctl(fd, EVIOCGKEY(sizeof(keys)), keys) < 0 ? -1 : volume_mask(keys);
}

JNIEXPORT jboolean JNICALL JNI_NAME(grab)(JNIEnv *env, jobject self, jint fd) {
    (void)env;
    (void)self;
    return ioctl(fd, EVIOCGRAB, 1) == 0;
}

JNIEXPORT jlong JNICALL JNI_NAME(readEdge)(JNIEnv *env, jobject self, jint fd) {
    (void)env;
    (void)self;
    struct input_event event;
    ssize_t size;
    do {
        size = read(fd, &event, sizeof(event));
    } while (size < 0 && errno == EINTR);
    if (size != sizeof(event) || (event.type == EV_SYN && event.code == SYN_DROPPED)) return -1;
    if (event.type != EV_KEY || (event.code != KEY_VOLUMEUP && event.code != KEY_VOLUMEDOWN) ||
        (event.value != 0 && event.value != 1)) return 0;
    // Low bits hold direction and down/up; the rest is the kernel's monotonic timestamp in ms.
    jlong time = (jlong)event.input_event_sec * 1000 + event.input_event_usec / 1000;
    return (time << 2) | (event.code == KEY_VOLUMEUP ? 2 : 0) | event.value;
}

JNIEXPORT jboolean JNICALL JNI_NAME(createOutput)(JNIEnv *env, jobject self, jint fd) {
    (void)env;
    (void)self;
    const struct uinput_setup setup = {
        .id = {.bustype = BUS_USB, .vendor = 0x18d2, .product = 0x7d7e, .version = 1},
        .name = "SandFly Volume Keys",
    };
    return ioctl(fd, UI_SET_EVBIT, EV_KEY) == 0 &&
           ioctl(fd, UI_SET_KEYBIT, KEY_VOLUMEUP) == 0 &&
           ioctl(fd, UI_SET_KEYBIT, KEY_VOLUMEDOWN) == 0 &&
           ioctl(fd, UI_DEV_SETUP, &setup) == 0 && ioctl(fd, UI_DEV_CREATE) == 0;
}

JNIEXPORT jboolean JNICALL JNI_NAME(emit)(JNIEnv *env, jobject self, jint fd, jboolean up_key,
                                        jboolean pressed) {
    (void)env;
    (void)self;
    const struct input_event events[] = {
        {.type = EV_KEY, .code = up_key ? KEY_VOLUMEUP : KEY_VOLUMEDOWN, .value = pressed ? 1 : 0},
        {.type = EV_SYN, .code = SYN_REPORT},
    };
    ssize_t size;
    do {
        size = write(fd, events, sizeof(events));
    } while (size < 0 && errno == EINTR);
    return size == sizeof(events);
}
