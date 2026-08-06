import React, { useState, useEffect, useRef } from 'react';
import { User, Sparkles } from 'lucide-react';

function AdditionalDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');

  const isSelfTriggeredRef = useRef(false);

  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    setName(user?.name || '');
    setEmail(user?.email || '');
  }, [user]);

  const syncChanges = (updatedName, updatedEmail) => {
    const updatedUser = {
      ...user,
      name: updatedName,
      email: updatedEmail
    };

    let updatedData = null;
    if (previewData) {
      updatedData = {
        ...previewData,
        applicant: {
          ...previewData.applicant,
          name: updatedName,
          email: updatedEmail
        }
      };
    } else {
      updatedData = {
        applicant: {
          name: updatedName,
          email: updatedEmail
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

  const handleNameChange = (val) => {
    isSelfTriggeredRef.current = true;
    setName(val);
    syncChanges(val, email);
  };

  const handleEmailChange = (val) => {
    isSelfTriggeredRef.current = true;
    setEmail(val);
    syncChanges(name, val);
  };

  return (
    <div className="card additional-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', display: 'flex', flexDirection: 'column' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px', flexShrink: 0 }}>
        <Sparkles className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Applicant Details</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {/* College Name */}
        <div className="form-group">
          <label className="form-label" htmlFor="details-name" style={{ color: '#4B5563', fontWeight: '600', fontSize: '12px', marginBottom: '4px', display: 'block' }}>
            College Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} size={16} />
            <input
              id="details-name"
              type="text"
              className="login-input"
              style={{
                background: '#F9FAFB',
                border: '1px solid #D1D5DB',
                color: '#1F2937',
                paddingLeft: '38px',
                paddingRight: '12px',
                height: '36px',
                fontSize: '13px',
                borderRadius: '6px',
                width: '100%'
              }}
              placeholder="Enter college name"
              value={name}
              onChange={(e) => handleNameChange(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Principal Name */}
        <div className="form-group">
          <label className="form-label" htmlFor="details-email" style={{ color: '#4B5563', fontWeight: '600', fontSize: '12px', marginBottom: '4px', display: 'block' }}>
            Principal Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} size={16} />
            <input
              id="details-email"
              type="text"
              className="login-input"
              style={{
                background: '#F9FAFB',
                border: '1px solid #D1D5DB',
                color: '#1F2937',
                paddingLeft: '38px',
                paddingRight: '12px',
                height: '36px',
                fontSize: '13px',
                borderRadius: '6px',
                width: '100%'
              }}
              placeholder="Enter principal name"
              value={email}
              onChange={(e) => handleEmailChange(e.target.value)}
              required
            />
          </div>
        </div>
      </div>
    </div>
  );
}

export default AdditionalDetailsCard;
