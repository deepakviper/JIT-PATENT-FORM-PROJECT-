import React, { useState } from 'react';
import Header from './components/Header';
import UploadCard from './components/UploadCard';
import PreviewDownloadCard from './components/PreviewDownloadCard';
import PatentFormsCard from './components/PatentFormsCard';
import AdditionalDetailsCard from './components/AdditionalDetailsCard';

function App() {
  // Track parsed patent data (initialized with default metadata to allow downloads instantly)
  const [parsedData, setParsedData] = useState({
    applicant: {
      name: '',
      email: ''
    },
    inventors: []
  });
  const [isDownloading, setIsDownloading] = useState(false);
  // Track which forms are selected in the right column
  const [selectedForms, setSelectedForms] = useState([]);
  // Track user login information (initialized to bypass login page)
  const [user, setUser] = useState({
    name: '',
    email: '',
    additionalMembers: []
  });

  // --- DOWNLOAD ACTION ---
  const handleDownloadDocx = async () => {
    if (!parsedData) return;
    
    if (selectedForms.length === 0) {
      alert("Please select at least one form to download from the list.");
      return;
    }

    // Includes form9 and form28 inside the execution lifecycle configuration
    const validForms = ['form1', 'form2', 'form3', 'form5', 'form9', 'form28'];
    const unsupportedSelected = selectedForms.filter(form => !validForms.includes(form));
    
    if (unsupportedSelected.length > 0) {
      alert(`Backend for ${unsupportedSelected.join(', ')} is not ready!`);
      return;
    }

    setIsDownloading(true);

    try {
      // Loop strictly through checked items only
      for (const formKey of selectedForms) {
        
        const response = await fetch(`http://localhost:8080/api/patent/download?formType=${formKey}`, {
          method: 'POST',
          headers: { 
            'Content-Type': 'application/json' 
          },
          body: JSON.stringify(parsedData), 
        });

        if (!response.ok) {
          throw new Error(`Server error during ${formKey} creation.`);
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        
        // Dynamically assign names based on the formType checked
        let displayFormName = 'Form_1_Application';
        if (formKey === 'form2') {
          displayFormName = 'Form_2_Specification';
        } else if (formKey === 'form3') {
          displayFormName = 'Form_3_Undertaking';
        } else if (formKey === 'form5') {
          displayFormName = 'Form_5_Declaration_of_Inventorship';
        } else if (formKey === 'form9') {
          displayFormName = 'Form_9_Request_For_Publication';
        } else if (formKey === 'form28') {
          displayFormName = 'Form_28_Small_Entity_Claim';
        }

        link.setAttribute('download', `Filled_Patent_${displayFormName}.docx`);
        
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
      }
    } catch (error) {
      console.error("Download Error: ", error);
      alert('An error occurred while downloading.');
    } finally {
      setIsDownloading(false);
    }
  };

  const handleResetWorkspace = () => {
    setUser({
      name: '',
      email: '',
      additionalMembers: []
    });
    setParsedData({
      applicant: {
        name: '',
        email: ''
      },
      inventors: []
    });
    setSelectedForms([]);
  };

  const handleDataParsed = (data) => {
    if (data) {
      // Merge: strictly use user's form inputs for applicant & inventors.
      // Other details (Title, Abstract, Claims, Description, Attachments) come from the parsed document.
      const mergedData = {
        ...data,
        applicant: {
          ...data.applicant,
          name: user?.name || '',
          email: user?.email || ''
        },
        inventors: (user?.additionalMembers || []).map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      };
      setParsedData(mergedData);
    } else {
      // No file uploaded: keep form inputs structure
      setParsedData({
        applicant: {
          name: user?.name || '',
          email: user?.email || ''
        },
        inventors: (user?.additionalMembers || []).map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      });
    }
  };

  return (
    <div>
      <Header user={user} onLogout={handleResetWorkspace} />
      <div className="main-container">
        <div className="left-column">
          <AdditionalDetailsCard
            previewData={parsedData}
            onChange={handleDataParsed}
            user={user}
            onUserUpdate={setUser}
          />
        </div>
        <div className="center-column">
          <UploadCard onDataParsed={handleDataParsed} />
          <PreviewDownloadCard 
            previewData={parsedData} 
            onDownloadTrigger={handleDownloadDocx}
            isDownloading={isDownloading}
            selectedForms={selectedForms} 
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