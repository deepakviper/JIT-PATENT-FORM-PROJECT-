import React from 'react';
import { Download, Loader2 } from 'lucide-react';

function PreviewDownloadCard({ previewData, onDownloadTrigger, isDownloading, selectedForms = [] }) {
  // Dynamically change text depending on how many forms are checked
  const buttonText = selectedForms.length > 1 ? 'Download Selected All' : 'Download';

  // Disabled if data isn't processed yet, downloading is in progress, or zero options are checked
  const isButtonDisabled = !previewData || isDownloading || selectedForms.length === 0;

  return (
    <div className="card preview-download-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', marginTop: '24px' }}>
      
      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}</style>

      <div style={{ textAlign: 'center', padding: '16px 16px 8px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <Download size={40} style={{ color: !isButtonDisabled ? '#0052cc' : '#9CA3AF', marginBottom: '12px' }} />
        <h4 style={{ margin: '0 0 8px 0', fontWeight: '600', fontSize: '1.05rem', color: !isButtonDisabled ? '#1F2937' : '#9CA3AF' }}>
          Download Filled Forms
        </h4>
        <p style={{ margin: '0 0 20px 0', fontSize: '0.85rem', color: '#6B7280', lineHeight: '1.4', maxWidth: '320px' }}>
          Download all checked patent specification sheets directly as highly structured Word Documents (.docx).
        </p>
        
        <button 
          className="action-btn"
          onClick={onDownloadTrigger} 
          disabled={isButtonDisabled}
          style={{ 
            padding: '12px 24px',
            backgroundColor: !isButtonDisabled ? '#0052cc' : '#E5E7EB',
            color: '#FFF',
            border: 'none',
            borderRadius: '6px',
            fontWeight: '600',
            cursor: !isButtonDisabled ? 'pointer' : 'not-allowed',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            width: '100%',
            maxWidth: '280px',
            justifyContent: 'center',
            boxShadow: !isButtonDisabled ? '0 2px 4px rgba(0, 82, 204, 0.2)' : 'none'
          }}
        >
          {isDownloading ? (
            <Loader2 className="spinning" style={{ animation: 'spin 1s linear infinite' }} size={16} />
          ) : (
            <Download size={16} />
          )}
          {buttonText}
        </button>
      </div>

    </div>
  );
}

export default PreviewDownloadCard;
