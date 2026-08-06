import React, { useState, useEffect, useRef } from 'react';
import { Phone, User } from 'lucide-react';

function PrincipalDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [principalName, setPrincipalName] = useState('');
  const [telephone, setTelephone] = useState('');
  const [mobile, setMobile] = useState('');
  const [fax, setFax] = useState('');
  const [email, setEmail] = useState('');

  const isSelfTriggeredRef = useRef(false);

  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    setPrincipalName(user?.email || '');
    const addr = user?.address || {};
    setTelephone(addr.telephone || '');
    setMobile(addr.mobile || '');
    setFax(addr.fax || '');
    setEmail(addr.email || '');
  }, [user]);

  const syncChanges = (updatedFields) => {
    isSelfTriggeredRef.current = true;

    const mergedAddress = {
      ...user?.address,
      telephone: updatedFields.hasOwnProperty('telephone') ? updatedFields.telephone : telephone,
      mobile: updatedFields.hasOwnProperty('mobile') ? updatedFields.mobile : mobile,
      fax: updatedFields.hasOwnProperty('fax') ? updatedFields.fax : fax,
      email: updatedFields.hasOwnProperty('email') ? updatedFields.email : email,
    };

    const newPrincipalName = updatedFields.hasOwnProperty('principalName') ? updatedFields.principalName : principalName;

    const updatedUser = {
      ...user,
      email: newPrincipalName,
      address: mergedAddress
    };

    let updatedData = null;
    if (previewData) {
      updatedData = {
        ...previewData,
        applicant: {
          ...previewData.applicant,
          email: newPrincipalName,
          address: {
            ...previewData.applicant?.address,
            ...mergedAddress
          }
        }
      };
    } else {
      updatedData = {
        applicant: {
          name: user?.name || '',
          email: newPrincipalName,
          address: mergedAddress
        },
        inventors: (user?.additionalMembers || []).map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      };
    }

    if (user && onUserUpdate) {
      onUserUpdate(updatedUser);
    }
    if (onChange) {
      onChange(updatedData);
    }
  };

  const handleFieldChange = (fieldName, val) => {
    if (fieldName === 'principalName') setPrincipalName(val);
    else if (fieldName === 'telephone') setTelephone(val);
    else if (fieldName === 'mobile') setMobile(val);
    else if (fieldName === 'fax') setFax(val);
    else if (fieldName === 'email') setEmail(val);

    syncChanges({ [fieldName]: val });
  };

  const inputStyle = {
    background: '#F9FAFB',
    border: '1px solid #D1D5DB',
    color: '#1F2937',
    paddingLeft: '12px',
    paddingRight: '12px',
    height: '36px',
    fontSize: '13px',
    borderRadius: '6px',
    width: '100%'
  };

  const labelStyle = {
    color: '#4B5563',
    fontWeight: '600',
    fontSize: '12px',
    marginBottom: '4px',
    display: 'block'
  };

  return (
    <div className="card principal-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', display: 'flex', flexDirection: 'column' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px', flexShrink: 0 }}>
        <Phone className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Principal Details</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        
        {/* Principal Name */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-principalName">Principal Name *</label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} size={16} />
            <input
              id="addr-principalName"
              type="text"
              className="login-input"
              style={{
                ...inputStyle,
                paddingLeft: '38px'
              }}
              placeholder="Enter principal name"
              value={principalName}
              onChange={(e) => handleFieldChange('principalName', e.target.value)}
              required
            />
          </div>
        </div>

        {/* Telephone No. */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-telephone">Telephone No.</label>
          <input
            id="addr-telephone"
            type="tel"
            className="login-input"
            style={inputStyle}
            placeholder="Enter telephone number"
            value={telephone}
            onChange={(e) => handleFieldChange('telephone', e.target.value)}
          />
        </div>

        {/* Mobile No. */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-mobile">Mobile No.</label>
          <input
            id="addr-mobile"
            type="tel"
            className="login-input"
            style={inputStyle}
            placeholder="Enter mobile number"
            value={mobile}
            onChange={(e) => handleFieldChange('mobile', e.target.value)}
          />
        </div>

        {/* Fax No. */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-fax">Fax No.</label>
          <input
            id="addr-fax"
            type="text"
            className="login-input"
            style={inputStyle}
            placeholder="Enter fax number"
            value={fax}
            onChange={(e) => handleFieldChange('fax', e.target.value)}
          />
        </div>

        {/* Email ID */}
        <div className="form-group">
          <label style={labelStyle} htmlFor="addr-email">Email ID</label>
          <input
            id="addr-email"
            type="email"
            className="login-input"
            style={inputStyle}
            placeholder="Enter email ID"
            value={email}
            onChange={(e) => handleFieldChange('email', e.target.value)}
          />
        </div>

      </div>
    </div>
  );
}

export default PrincipalDetailsCard;
