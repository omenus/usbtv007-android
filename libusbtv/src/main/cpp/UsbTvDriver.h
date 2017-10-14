//
// Created by Eric on 10/12/2017.
//

#ifndef USBTV007_ANDROID_USBTVDRIVER_H
#define USBTV007_ANDROID_USBTVDRIVER_H

#include "usbtv_definitions.h"
#include "AndroidUsbDevice.h"
#include <pthread.h>

class UsbTvDriver {
private:
	bool _initalized; // Variable to check to make sure constructor successfully completed

	/* Video Members */
	TvInput     _input;
	TvNorm      _tvNorm;
	ScanType    _scanType;

	UsbTvFrame** _framePool;

	AndroidUsbDevice*   _usbConnection;
	bool                _useCallback;
	bool                _shouldRender;
	jobject             _renderSurface;

	bool _streamActive;

	uint16_t _frameWidth;
	uint16_t _frameHeight;

	// Isonchronous Transfer Variables
	uint8_t     _isoEndpoint;
	uint8_t     _numIsoPackets;
	uint32_t    _maxIsoPacketSize;

	// packet/frame tracking variables
	UsbTvFrame* _usbInputFrame;      // The current frame being written to from Usb
	uint32_t    _currentFrameId;
	uint16_t    _packetsPerField;
	uint16_t    _packetsDone;
	bool        _secondFrame;

	// Frame Process variables
	ThreadContext*      _frameProcessContext;
	UsbTvFrame*         _processFrame;    // The current frame being processed
	pthread_t*          _frameProcessThread;
	pthread_mutex_t     _frameProcessMutex;
	pthread_cond_t      _frameReadyCond;
	bool                _frameWait;


	/* TODO: Audio Members */

	/* Private Member Functions */
	bool setRegisters(const uint16_t regs[][2], int size);
	UsbTvFrame* fetchFrameFromPool();
	void allocateFramePool();
	void freeFramePool();

	// Callback executed when a urb is received from USB
	void onUrbReceived(usbdevfs_urb* urb);
	void processPacket(__be32* packet);
	void packetToProgressiveFrame(uint8_t* packet, uint32_t packetNo);
	void packetToInterleavedFrame(uint8_t* packet, uint32_t packetNo, bool isOdd);
	void notifyFrameComplete();

public:
	struct ThreadContext {
		UsbTvDriver*    usbtv;
		JavaCallback*   callback;
		bool*           useCallback;
		bool*           shouldRender;
	};
	UsbTvDriver(JNIEnv *env, jobject thisObj, int fd, int isoEndpoint, int maxIsoPacketSize,
	            int input, int norm, int scanType);
	~UsbTvDriver();

	bool isInitialized() { return _initalized;}
	bool isStreaming() { return _streamActive;};


	void setCallback(bool shouldUse) {_useCallback = shouldUse;}
	void setSurface(jobject surface);

	UsbTvFrame* getFrame();

	bool startStreaming();
	void stopStreaming();
	bool setTvNorm(int norm);
	bool setTvInput(int input);
	bool setScanType(int scanType);
	bool setControl(int control, int value);
	int  getControl(int control);

};


#endif //USBTV007_ANDROID_USBTVDRIVER_H