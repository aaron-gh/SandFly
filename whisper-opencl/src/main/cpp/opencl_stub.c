// Copyright 2026 SandFly contributors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You may obtain a copy of
// the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations under
// the License.

// The functions of OpenCL's cl.h, only for linking against: the build makes libOpenCL.so from
// this, which is not packaged. On the phone, the GPU vendor's libOpenCL.so provides them.
// Generated from opencl/CL/cl.h.

void clBuildProgram(void) {}
void clCloneKernel(void) {}
void clCompileProgram(void) {}
void clCreateBuffer(void) {}
void clCreateBufferWithProperties(void) {}
void clCreateCommandQueue(void) {}
void clCreateCommandQueueWithProperties(void) {}
void clCreateContext(void) {}
void clCreateContextFromType(void) {}
void clCreateImage(void) {}
void clCreateImage2D(void) {}
void clCreateImage3D(void) {}
void clCreateImageWithProperties(void) {}
void clCreateKernel(void) {}
void clCreateKernelsInProgram(void) {}
void clCreatePipe(void) {}
void clCreateProgramWithBinary(void) {}
void clCreateProgramWithBuiltInKernels(void) {}
void clCreateProgramWithIL(void) {}
void clCreateProgramWithSource(void) {}
void clCreateSampler(void) {}
void clCreateSamplerWithProperties(void) {}
void clCreateSubBuffer(void) {}
void clCreateSubDevices(void) {}
void clCreateUserEvent(void) {}
void clEnqueueBarrier(void) {}
void clEnqueueBarrierWithWaitList(void) {}
void clEnqueueCopyBuffer(void) {}
void clEnqueueCopyBufferRect(void) {}
void clEnqueueCopyBufferToImage(void) {}
void clEnqueueCopyImage(void) {}
void clEnqueueCopyImageToBuffer(void) {}
void clEnqueueFillBuffer(void) {}
void clEnqueueFillImage(void) {}
void clEnqueueMapBuffer(void) {}
void clEnqueueMapImage(void) {}
void clEnqueueMarker(void) {}
void clEnqueueMarkerWithWaitList(void) {}
void clEnqueueMigrateMemObjects(void) {}
void clEnqueueNDRangeKernel(void) {}
void clEnqueueNativeKernel(void) {}
void clEnqueueReadBuffer(void) {}
void clEnqueueReadBufferRect(void) {}
void clEnqueueReadImage(void) {}
void clEnqueueSVMFree(void) {}
void clEnqueueSVMMap(void) {}
void clEnqueueSVMMemFill(void) {}
void clEnqueueSVMMemcpy(void) {}
void clEnqueueSVMMigrateMem(void) {}
void clEnqueueSVMUnmap(void) {}
void clEnqueueTask(void) {}
void clEnqueueUnmapMemObject(void) {}
void clEnqueueWaitForEvents(void) {}
void clEnqueueWriteBuffer(void) {}
void clEnqueueWriteBufferRect(void) {}
void clEnqueueWriteImage(void) {}
void clFinish(void) {}
void clFlush(void) {}
void clGetCommandQueueInfo(void) {}
void clGetContextInfo(void) {}
void clGetDeviceAndHostTimer(void) {}
void clGetDeviceIDs(void) {}
void clGetDeviceInfo(void) {}
void clGetEventInfo(void) {}
void clGetEventProfilingInfo(void) {}
void clGetExtensionFunctionAddress(void) {}
void clGetExtensionFunctionAddressForPlatform(void) {}
void clGetHostTimer(void) {}
void clGetImageInfo(void) {}
void clGetKernelArgInfo(void) {}
void clGetKernelInfo(void) {}
void clGetKernelSubGroupInfo(void) {}
void clGetKernelSuggestedLocalWorkSize(void) {}
void clGetKernelWorkGroupInfo(void) {}
void clGetMemObjectInfo(void) {}
void clGetPipeInfo(void) {}
void clGetPlatformIDs(void) {}
void clGetPlatformInfo(void) {}
void clGetProgramBuildInfo(void) {}
void clGetProgramInfo(void) {}
void clGetSamplerInfo(void) {}
void clGetSupportedImageFormats(void) {}
void clLinkProgram(void) {}
void clReleaseCommandQueue(void) {}
void clReleaseContext(void) {}
void clReleaseDevice(void) {}
void clReleaseEvent(void) {}
void clReleaseKernel(void) {}
void clReleaseMemObject(void) {}
void clReleaseProgram(void) {}
void clReleaseSampler(void) {}
void clRetainCommandQueue(void) {}
void clRetainContext(void) {}
void clRetainDevice(void) {}
void clRetainEvent(void) {}
void clRetainKernel(void) {}
void clRetainMemObject(void) {}
void clRetainProgram(void) {}
void clRetainSampler(void) {}
void clSVMAlloc(void) {}
void clSVMFree(void) {}
void clSetCommandQueueProperty(void) {}
void clSetContextDestructorCallback(void) {}
void clSetDefaultDeviceCommandQueue(void) {}
void clSetEventCallback(void) {}
void clSetKernelArg(void) {}
void clSetKernelArgSVMPointer(void) {}
void clSetKernelExecInfo(void) {}
void clSetMemObjectDestructorCallback(void) {}
void clSetProgramReleaseCallback(void) {}
void clSetProgramSpecializationConstant(void) {}
void clSetUserEventStatus(void) {}
void clUnloadCompiler(void) {}
void clUnloadPlatformCompiler(void) {}
void clWaitForEvents(void) {}
