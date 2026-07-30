import React, { useState } from 'react';
import Header from './components/Header';
import UploadCard from './components/UploadCard';
import PreviewDownloadCard from './components/PreviewDownloadCard';
import PatentFormsCard from './components/PatentFormsCard';

function App() {
  const [parsedData, setParsedData] = useState(null);
  const [isDownloading, setIsDownloading] = useState(false);
  // Track which forms are selected in the right column
  const [selectedForms, setSelectedForms] = useState([]);

  // --- DOWNLOAD ACTION ---
  const handleDownloadDocx = async () => {
    if (!parsedData) return;
    
    if (selectedForms.length === 0) {
      alert("Please select at least one form to download from the list.");
      return;
    }

    // Temporary Safeguard: Alert if they chose forms you haven't coded in the backend yet
    const hasUnimplementedForms = selectedForms.some(formId => formId !== 'form1');
    if (hasUnimplementedForms && selectedForms.includes('form1')) {
      alert("Note: Future forms are selected, but only Form 1 will be generated for now since the backend supports Form 1.");
    } else if (hasUnimplementedForms && !selectedForms.includes('form1')) {
      alert("Backend for these forms is not implemented yet! Please select Form Page 1.");
      return;
    }

    setIsDownloading(true);

    try {
      const downloadResponse = await fetch('http://localhost:8080/api/patent/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...parsedData,         // Spreads fields (title, abstract, claims) at root level for backend binding
          requestedForms: selectedForms 
        }),
      });

      if (!downloadResponse.ok) throw new Error('Server error during document creation.');

      const blob = await downloadResponse.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = downloadUrl;
      
      // Dynamic naming structure based on number of selected forms
      const filename = selectedForms.length === 1 
        ? `Filled_Patent_${selectedForms[0]}.docx` 
        : 'Filled_Patent_Documents.zip'; 
        
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      console.error(error);
      alert('An error occurred while downloading.');
    } finally {
      setIsDownloading(false);
    }
  };

  const handleDataParsed = (data) => {
    setParsedData(data);
  };

  return (
    <div>
      <Header />
      <div className="main-container">
        <div className="left-column">
          <UploadCard onDataParsed={handleDataParsed} />
          <PreviewDownloadCard 
            previewData={parsedData} 
            onDownloadTrigger={handleDownloadDocx}
            isDownloading={isDownloading}
            selectedForms={selectedForms} // 👈 Added prop to update button text dynamically
          />
        </div>
        <div className="right-column">
          <PatentFormsCard 
            selectedForms={selectedForms} 
            setSelectedForms={setSelectedForms} 
          />
        </div>
      </div>
    </div>
  );
}

export default App;