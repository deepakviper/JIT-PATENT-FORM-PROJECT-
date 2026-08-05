import React, { useState, useEffect, useRef } from 'react';
import { User, Users, Sparkles, Plus } from 'lucide-react';

function AdditionalDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [coApplicants, setCoApplicants] = useState([]);

  // Use a ref to track whether the update was self-triggered to prevent re-render lag
  const isSelfTriggeredRef = useRef(false);
  const listEndRef = useRef(null);

  // Initialize and synchronize state when user changes
  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    setName(user?.name || '');
    setEmail(user?.email || '');
    
    const members = user?.additionalMembers || [];
    const padded = [...members];
    while (padded.length < 3) {
      padded.push({ name: '' });
    }
    setCoApplicants(padded);
  }, [user]);

  const syncChanges = (updatedName, updatedEmail, updatedMembers) => {
    // Filter out members that have empty names for the backend/count sync
    const activeMembers = updatedMembers.filter(m => m && m.name && m.name.trim() !== '');

    const updatedUser = {
      ...user,
      name: updatedName,
      email: updatedEmail,
      extraPersonsCount: activeMembers.length,
      additionalMembers: updatedMembers
    };

    let updatedData = null;
    if (previewData) {
      updatedData = {
        ...previewData,
        applicant: {
          ...previewData.applicant,
          name: updatedName,
          email: updatedEmail
        },
        inventors: activeMembers.map(m => ({
          name: m.name,
          nationality: 'Indian',
          country: 'India'
        }))
      };
    } else {
      updatedData = {
        applicant: {
          name: updatedName,
          email: updatedEmail
        },
        inventors: activeMembers.map(m => ({
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
    syncChanges(val, email, coApplicants);
  };

  const handleEmailChange = (val) => {
    isSelfTriggeredRef.current = true;
    setEmail(val);
    syncChanges(name, val, coApplicants);
  };

  const handleAddMember = () => {
    isSelfTriggeredRef.current = true;
    if (coApplicants.length >= 8) {
      alert("You can add up to 8 inventors.");
      return;
    }
    const newCoApplicants = [...coApplicants, { name: '' }];
    setCoApplicants(newCoApplicants);
    syncChanges(name, email, newCoApplicants);

    // Smoothly scroll down so the user sees the newly added inventor
    setTimeout(() => {
      listEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 100);
  };

  const handleRemoveMember = (index) => {
    isSelfTriggeredRef.current = true;
    // We cannot drop below 3 elements in the visible state array
    let newCoApplicants = coApplicants.filter((_, idx) => idx !== index);
    while (newCoApplicants.length < 3) {
      newCoApplicants.push({ name: '' });
    }
    setCoApplicants(newCoApplicants);
    syncChanges(name, email, newCoApplicants);
  };

  const handleMemberChange = (index, value) => {
    isSelfTriggeredRef.current = true;
    const newCoApplicants = coApplicants.map((member, idx) => {
      if (idx === index) {
        return { name: value };
      }
      return member;
    });
    setCoApplicants(newCoApplicants);
    syncChanges(name, email, newCoApplicants);
  };

  return (
    <div className="card additional-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB', display: 'flex', flexDirection: 'column', height: '100%', minHeight: '520px', maxHeight: '680px', overflow: 'hidden' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px', flexShrink: 0 }}>
        <Sparkles className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Applicant Details</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', flex: 1, overflowY: 'auto', paddingRight: '4px' }}>
        {/* College Name */}
        <div className="form-group">
          <label className="form-label" htmlFor="details-name" style={{ color: '#4B5563', fontWeight: '600', fontSize: '12px' }}>
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
                borderRadius: '6px'
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
          <label className="form-label" htmlFor="details-email" style={{ color: '#4B5563', fontWeight: '600', fontSize: '12px' }}>
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
                borderRadius: '6px'
              }}
              placeholder="Enter principal name"
              value={email}
              onChange={(e) => handleEmailChange(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Inventors Header */}
        <div style={{
          marginTop: '6px',
          borderTop: '1px solid #F3F4F6',
          paddingTop: '12px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span style={{ fontSize: '13px', fontWeight: '600', color: '#1F2937', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Users size={16} style={{ color: '#0052cc' }} /> Inventors
          </span>
        </div>

        {/* List of Inventors */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {/* First 3 inventors in one row */}
          <div style={{ display: 'flex', gap: '12px', width: '100%', flexWrap: 'wrap' }}>
            {coApplicants.slice(0, 3).map((member, index) => (
              <div 
                key={index} 
                className="form-group"
                style={{ 
                  flex: '1 1 calc(33.333% - 8px)',
                  minWidth: '120px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '4px'
                }}
              >
                <label className="form-label" htmlFor={`member-name-${index}`} style={{ fontSize: '11px', color: '#4B5563', fontWeight: '600', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  Inventor #{index + 1} {index === 0 ? '*' : '(Optional)'}
                </label>
                <input
                  id={`member-name-${index}`}
                  type="text"
                  className="login-input"
                  style={{
                    background: '#F9FAFB',
                    border: '1px solid #D1D5DB',
                    color: '#1F2937',
                    paddingLeft: '12px',
                    paddingRight: '12px',
                    height: '36px',
                    fontSize: '13px',
                    borderRadius: '6px',
                    width: '100%'
                  }}
                  placeholder={`Enter inventor #${index + 1} name`}
                  value={member.name || ''}
                  onChange={(e) => handleMemberChange(index, e.target.value)}
                  required={index === 0}
                />
              </div>
            ))}
          </div>

          {/* Additional inventors, starting from index 3 */}
          {coApplicants.length > 3 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {coApplicants.slice(3).map((member, idx) => {
                const index = idx + 3;
                return (
                  <div 
                    key={index} 
                    className="form-group"
                    style={{ 
                      animation: 'slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '4px'
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <label className="form-label" htmlFor={`member-name-${index}`} style={{ fontSize: '11px', color: '#4B5563', fontWeight: '600' }}>
                        Inventor #{index + 1}
                      </label>
                      <button
                        type="button"
                        onClick={() => handleRemoveMember(index)}
                        style={{
                          background: 'transparent',
                          color: '#EF4444',
                          border: 'none',
                          fontSize: '11px',
                          fontWeight: '600',
                          cursor: 'pointer',
                          padding: '2px 6px',
                          borderRadius: '4px',
                          transition: 'background 0.2s'
                        }}
                      >
                        Remove
                      </button>
                    </div>
                    <input
                      id={`member-name-${index}`}
                      type="text"
                      className="login-input"
                      style={{
                        background: '#F9FAFB',
                        border: '1px solid #D1D5DB',
                        color: '#1F2937',
                        paddingLeft: '12px',
                        paddingRight: '12px',
                        height: '36px',
                        fontSize: '13px',
                        borderRadius: '6px'
                      }}
                      placeholder={`Enter inventor #${index + 1} name`}
                      value={member.name || ''}
                      onChange={(e) => handleMemberChange(index, e.target.value)}
                    />
                  </div>
                );
              })}
            </div>
          )}
          {/* Ref element used for automatic scroll positioning */}
          <div ref={listEndRef} />
        </div>

        {/* Add button */}
        {coApplicants.length < 8 && (
          <div style={{ display: 'flex', justifyContent: 'center', marginTop: '12px' }}>
            <button
              type="button"
              onClick={handleAddMember}
              style={{
                background: '#0052cc',
                color: '#FFF',
                border: 'none',
                padding: '8px 16px',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '4px',
                transition: 'background 0.2s'
              }}
            >
              <Plus size={14} /> Add Inventor
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default AdditionalDetailsCard;
