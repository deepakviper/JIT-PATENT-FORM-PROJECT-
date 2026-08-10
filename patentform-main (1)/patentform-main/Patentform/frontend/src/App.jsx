import React, { useState } from 'react';
import Header from './components/Header';
import UploadCard from './components/UploadCard';
import PreviewDownloadCard from './components/PreviewDownloadCard';
import PatentFormsCard from './components/PatentFormsCard';
import AdditionalDetailsCard from './components/AdditionalDetailsCard';
import PrincipalDetailsCard from './components/PrincipalDetailsCard';
import InventorsCard from './components/InventorsCard';

function App() {
  // Track parsed patent data (initialized with default metadata to allow downloads instantly)
  const [parsedData, setParsedData] = useState({
    applicant: {
      name: '',
      email: '',
      address: {
        houseNo: '',
        street: '',
        city: '',
        state: '',
        country: '',
        pincode: '',
        principalName: '',
        telephone: '',
        mobile: '',
        fax: '',
        email: ''
      }
    },
    inventors: []
  });
  const [isDownloading, setIsDownloading] = useState(false);
  // Track which forms are selected in the right column
  const [selectedForms, setSelectedForms] = useState([]);
  // Track the raw source file for Form 2 generation
  const [sourceFile, setSourceFile] = useState(null);
  // Track user login information (initialized to bypass login page)
  const [user, setUser] = useState({
    name: '',
    email: '',
    additionalMembers: [],
    address: {
      houseNo: '',
      street: '',
      city: '',
      state: '',
      country: '',
      pincode: '',
      principalName: '',
      telephone: '',
      mobile: '',
      fax: '',
      email: ''
    }
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
        const formData = new FormData();
        formData.append('data', JSON.stringify(parsedData));
        if (sourceFile) {
          formData.append('sourceFile', sourceFile);
        }
        
        const response = await fetch(`${import.meta.env.VITE_API_URL}/api/patent/download?formType=${formKey}`, {
          method: 'POST',
          body: formData,
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
    setSourceFile(null);
    setUser({
      name: '',
      email: '',
      additionalMembers: [],
      address: {
        houseNo: '',
        street: '',
        city: '',
        state: '',
        country: '',
        pincode: '',
        principalName: '',
        telephone: '',
        mobile: '',
        fax: '',
        email: ''
      }
    });
    setParsedData({
      applicant: {
        name: '',
        email: '',
        address: {
          houseNo: '',
          street: '',
          city: '',
          state: '',
          country: '',
          pincode: '',
          principalName: '',
          telephone: '',
          mobile: '',
          fax: '',
          email: ''
        }
      },
      inventors: []
    });
    setSelectedForms([]);
  };

  const handleDataParsed = (data, file) => {
    if (file) {
      setSourceFile(file);
    }
    if (data) {
      const parsedAddress = data.applicant?.address || {};

      setUser(prevUser => {
        const updatedAddress = {
          ...prevUser.address,
          street: prevUser.address.street || parsedAddress.street || '',
          city: prevUser.address.city || parsedAddress.city || '',
          state: prevUser.address.state || parsedAddress.state || '',
          country: prevUser.address.country || parsedAddress.country || '',
          pincode: prevUser.address.pincode || parsedAddress.pincode || ''
        };
        return {
          ...prevUser,
          address: updatedAddress
        };
      });

      // Merge: strictly use user's form inputs for applicant & inventors.
      // Other details (Title, Abstract, Claims, Description, Attachments) come from the parsed document.
      const mergedData = {
        ...data,
        applicant: {
          ...data.applicant,
          name: user?.name || data.applicant?.name || '',
          email: user?.email || data.applicant?.email || '',
          address: {
            ...data.applicant?.address,
            houseNo: user?.address?.houseNo || '',
            street: user?.address?.street || data.applicant?.address?.street || '',
            city: user?.address?.city || data.applicant?.address?.city || '',
            state: user?.address?.state || data.applicant?.address?.state || '',
            country: user?.address?.country || data.applicant?.address?.country || '',
            pincode: user?.address?.pincode || data.applicant?.address?.pincode || '',
            principalName: user?.address?.principalName || '',
            telephone: user?.address?.telephone || '',
            mobile: user?.address?.mobile || '',
            fax: user?.address?.fax || '',
            email: user?.address?.email || ''
          }
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
          email: user?.email || '',
          address: {
            houseNo: user?.address?.houseNo || '',
            street: user?.address?.street || '',
            city: user?.address?.city || '',
            state: user?.address?.state || '',
            country: user?.address?.country || '',
            pincode: user?.address?.pincode || '',
            principalName: user?.address?.principalName || '',
            telephone: user?.address?.telephone || '',
            mobile: user?.address?.mobile || '',
            fax: user?.address?.fax || '',
            email: user?.address?.email || ''
          }
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
        <div className="column-card-container col-1">
          <AdditionalDetailsCard
            previewData={parsedData}
            onChange={handleDataParsed}
            user={user}
            onUserUpdate={setUser}
          />
        </div>
        <div className="column-card-container col-2" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <PrincipalDetailsCard
            previewData={parsedData}
            onChange={handleDataParsed}
            user={user}
            onUserUpdate={setUser}
          />
          <InventorsCard
            previewData={parsedData}
            onChange={handleDataParsed}
            user={user}
            onUserUpdate={setUser}
          />
        </div>
        <div className="column-card-container col-3" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <UploadCard onDataParsed={handleDataParsed} />
          <PreviewDownloadCard 
            previewData={parsedData} 
            onDownloadTrigger={handleDownloadDocx}
            isDownloading={isDownloading}
            selectedForms={selectedForms} 
          />
        </div>
        <div className="column-card-container col-4">
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