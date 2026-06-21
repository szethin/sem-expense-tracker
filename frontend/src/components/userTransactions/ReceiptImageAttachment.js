import { useEffect, useRef, useState } from 'react';

const MAX_FILE_SIZE = 10 * 1024 * 1024;

function detectMimeType(base64) {
    if (base64.startsWith('/9j/')) {
        return 'image/jpeg';
    }
    if (base64.startsWith('iVBOR')) {
        return 'image/png';
    }
    
    return 'image/jpeg';
}

function base64ToObjectUrl(base64) {
    const binaryString = window.atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    const blob = new Blob([bytes], { type: detectMimeType(base64) });
    return URL.createObjectURL(blob);
}

function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            const result = reader.result;
            const base64 = result.includes(',') ? result.split(',')[1] : result;
            resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });
}

function ReceiptImageAttachment({ existingImageBase64, onChange }) {
    const uploadInputRef = useRef(null);
    const videoRef = useRef(null);
    const previewUrlRef = useRef(null);
    const streamRef = useRef(null);
    const replacedExistingRef = useRef(false);
    const existingLoadedRef = useRef(false);

    const [previewUrl, setPreviewUrl] = useState(null);
    const [imageBase64, setImageBase64] = useState(null);
    const [removeExisting, setRemoveExisting] = useState(false);
    const [error, setError] = useState('');
    const [zoomOpen, setZoomOpen] = useState(false);
    const [cameraOpen, setCameraOpen] = useState(false);

    const revokePreviewUrl = () => {
        if (previewUrlRef.current) {
            URL.revokeObjectURL(previewUrlRef.current);
            previewUrlRef.current = null;
        }
    };

    const setPreviewFromUrl = (url) => {
        revokePreviewUrl();
        previewUrlRef.current = url;
        setPreviewUrl(url);
    };

    useEffect(() => {
        if (existingImageBase64 && !existingLoadedRef.current) {
            setPreviewFromUrl(base64ToObjectUrl(existingImageBase64));
            existingLoadedRef.current = true;
        }
    }, [existingImageBase64]);

    useEffect(() => {
        onChange({
            receiptImage: imageBase64,
            removeReceiptImage: removeExisting && !imageBase64
        });
    }, [imageBase64, removeExisting, onChange]);

    useEffect(() => {
        return () => {
            revokePreviewUrl();
            stopCamera();
        };
    }, []);

    useEffect(() => {
        if (cameraOpen && videoRef.current && streamRef.current) {
            videoRef.current.srcObject = streamRef.current;
            videoRef.current.play().catch(() => {
                setError('Unable to start camera. Please try again.');
                stopCamera();
            });
        }
    }, [cameraOpen]);

    const stopCamera = () => {
        if (streamRef.current) {
            streamRef.current.getTracks().forEach((track) => track.stop());
            streamRef.current = null;
        }
        if (videoRef.current) {
            videoRef.current.srcObject = null;
        }
        setCameraOpen(false);
    };

    const applySelectedFile = async (file) => {
        if (existingImageBase64) {
            replacedExistingRef.current = true;
        }

        setRemoveExisting(true);

        const base64 = await fileToBase64(file);
        setImageBase64(base64);
        setPreviewFromUrl(URL.createObjectURL(file));
    };

    const handleFileSelect = async (file) => {
        if (!file) {
            return;
        }

        if (!file.type.startsWith('image/')) {
            setError('Please select a valid image file.');
            return;
        }

        if (file.size > MAX_FILE_SIZE) {
            setError('Image must not exceed 10MB.');
            return;
        }

        setError('');
        await applySelectedFile(file);
    };

    const handleUploadChange = (event) => {
        handleFileSelect(event.target.files[0]);
        event.target.value = '';
    };

    const openDeviceCamera = async () => {
        setError('');

        if (!navigator.mediaDevices?.getUserMedia) {
            setError('Camera is not supported on this device.');
            return;
        }

        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: true,
                audio: false
            });
            streamRef.current = stream;
            setCameraOpen(true);
        } catch (cameraError) {
            setError('Unable to access camera. Please allow camera permission and try again.');
        }
    };

    const capturePhoto = () => {
        const video = videoRef.current;
        if (!video || !video.videoWidth || !video.videoHeight) {
            setError('Camera is not ready yet. Please try again.');
            return;
        }

        const canvas = document.createElement('canvas');
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        canvas.getContext('2d').drawImage(video, 0, 0);

        canvas.toBlob(async (blob) => {
            stopCamera();

            if (!blob) {
                setError('Failed to capture photo. Please try again.');
                return;
            }

            if (blob.size > MAX_FILE_SIZE) {
                setError('Captured image must not exceed 10MB.');
                return;
            }

            setError('');
            const file = new File([blob], `receipt-${Date.now()}.jpg`, { type: 'image/jpeg' });
            await applySelectedFile(file);
        }, 'image/jpeg', 0.92);
    };

    const handleRemoveImage = (event) => {
        event.stopPropagation();
        revokePreviewUrl();
        setPreviewUrl(null);
        setImageBase64(null);

        if (existingImageBase64 || replacedExistingRef.current) {
            setRemoveExisting(true);
        }
    };

    const openZoom = () => {
        if (previewUrl) {
            setZoomOpen(true);
        }
    };

    const hasPreview = Boolean(previewUrl);

    return (
        <div className='input-box receipt-attachment'>
            <label>Image (optional)</label>
            <div className={`receipt-attachment-container${hasPreview ? ' has-image' : ''}`}>
                <div className='receipt-attachment-actions'>
                    <button
                        type='button'
                        className='button outline receipt-action-btn'
                        onClick={() => uploadInputRef.current?.click()}
                    >
                        Upload from device
                    </button>
                    <button
                        type='button'
                        className='button outline receipt-action-btn'
                        onClick={openDeviceCamera}
                    >
                        Take photo
                    </button>
                </div>

                <input
                    ref={uploadInputRef}
                    type='file'
                    accept='image/*'
                    hidden
                    onChange={handleUploadChange}
                />

                {hasPreview && (
                    <div className='receipt-preview-area'>
                        <div className='receipt-preview-wrapper'>
                            <button
                                type='button'
                                className='receipt-remove-btn'
                                onClick={handleRemoveImage}
                                aria-label='Remove receipt image'
                            >
                                ×
                            </button>
                            <img
                                src={previewUrl}
                                alt='Receipt preview'
                                className='receipt-preview-image'
                                onClick={openZoom}
                            />
                        </div>
                    </div>
                )}

                {error && <small className='receipt-error'>{error}</small>}
            </div>

            {cameraOpen && (
                <div className='receipt-camera-overlay' onClick={stopCamera}>
                    <div className='receipt-camera-content' onClick={(event) => event.stopPropagation()}>
                        <p className='receipt-camera-title'>Position your receipt, then capture</p>
                        <video ref={videoRef} className='receipt-camera-video' autoPlay playsInline muted />
                        <div className='receipt-camera-actions'>
                            <button type='button' className='button outline receipt-action-btn' onClick={stopCamera}>
                                Cancel
                            </button>
                            <button type='button' className='button button-fill receipt-action-btn' onClick={capturePhoto}>
                                Capture photo
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {zoomOpen && (
                <div className='receipt-zoom-overlay' onClick={() => setZoomOpen(false)}>
                    <img
                        src={previewUrl}
                        alt='Receipt zoomed'
                        className='receipt-zoom-image'
                        onClick={(event) => event.stopPropagation()}
                    />
                </div>
            )}
        </div>
    );
}

export default ReceiptImageAttachment;
