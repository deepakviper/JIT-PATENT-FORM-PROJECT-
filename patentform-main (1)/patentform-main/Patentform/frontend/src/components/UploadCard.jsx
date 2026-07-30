import React, { useState, useRef, useCallback } from 'react';
import { UploadCloud, FolderOpen, File, CheckCircle, X, Loader2 } from 'lucide-react';

function UploadCard({ onDataParsed }) {
  const [isDragging, setIsDragging] = useState(false);
  const [uploadedFile, setUploadedFile] = useState(null);
  const [hasData, setHasData] = useState(false);
  
  // Keep the loading state local to this component to avoid parent dependency crashes
  const [isGenerating, setIsGenerating] = useState(false);
  
  const fileInputRef = useRef(null);

  const handleDragOver = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      const file = files[0];
      if (file.name.endsWith('.docx')) {
        setUploadedFile(file);
        setHasData(false);
        if (onDataParsed) onDataParsed(null); 
      } else {
        alert('Please upload a .docx file.');
      }
    }
  }, [onDataParsed]);

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e) => {
    const files = e.target.files;
    if (files.length > 0) {
      const file = files[0];
      if (file.name.endsWith('.docx')) {
        setUploadedFile(file);
        setHasData(false);
        if (onDataParsed) onDataParsed(null); 
      } else {
        alert('Please upload a .docx file.');
      }
    }
    e.target.value = '';
  };

  const handleRemoveFile = () => {
    setUploadedFile(null);
    setHasData(false);
    if (onDataParsed) onDataParsed(null);
  };

  const handleGeneratePreview = async () => {
    if (!uploadedFile) return;

    setIsGenerating(true);

    try {
      const formData = new FormData();
      formData.append('file', uploadedFile);

      const parseResponse = await fetch('http://localhost:8080/api/patent/parse', {
        method: 'POST',
        body: formData,
      });

      if (!parseResponse.ok) {
        throw new Error('Server error while parsing document contents.');
      }

      const parsedResponseData = await parseResponse.json();
      setHasData(true);
      if (onDataParsed) {
        onDataParsed(parsedResponseData); 
      }

    } catch (error) {
      console.error('Parsing pipeline error:', error);
      alert('An error occurred during extraction. Check if backend is running.');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div
      className={`card upload-card ${isDragging ? 'dragging' : ''}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{ width: '100%' }}
    >
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept=".docx"
        style={{ display: 'none' }}
      />

      {isGenerating ? (
        <div className="upload-loading-container" style={{ textAlign: 'center', padding: '20px' }}>
          <Loader2 className="upload-icon spinning" style={{ animation: 'spin 1.5s linear infinite' }} />
          <div className="upload-title" style={{ marginTop: '15px' }}>Processing Document...</div>
          <div className="upload-instruction">Parsing contents and generating your filled patent form.</div>
          
          <style>{`
            @keyframes spin {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
          `}</style>
        </div>
      ) : uploadedFile ? (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <CheckCircle className="upload-icon success-icon" />
          <div className="upload-title">File Uploaded</div>
          <div className="file-info">
            <File className="file-icon" />
            <span className="file-name">{uploadedFile.name}</span>
            <button className="file-remove-btn" onClick={handleRemoveFile} title="Remove file">
              <X size={16} />
            </button>
          </div>
          
          {!hasData && (
            <button 
              className="browse-btn" 
              onClick={handleGeneratePreview}
              style={{ marginTop: '20px', backgroundColor: '#0052cc', color: '#FFF' }}
            >
              Generate
            </button>
          )}
          {hasData && (
            <div style={{ marginTop: '15px', color: '#10B981', fontWeight: '600', fontSize: '0.9rem' }}>
              ✓ Extraction complete! Please select the form.
            </div>
          )}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <UploadCloud className="upload-icon" />
          <div className="upload-title">Upload Patent Proposal</div>
          <div className="upload-instruction">Drag & Drop your Word document here</div>
          <div className="upload-divider">or</div>
          <button className="browse-btn" onClick={handleBrowseClick}>
            <FolderOpen />
            Browse Files
          </button>
          <div className="upload-footer">Supported format: .docx</div>
        </div>
      )}
    </div>
  );
}

export default UploadCard;