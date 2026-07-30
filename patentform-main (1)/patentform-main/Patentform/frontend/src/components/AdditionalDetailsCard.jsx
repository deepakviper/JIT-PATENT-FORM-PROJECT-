import React, { useState, useEffect, useRef } from 'react';
import { User, Mail, Users, Sparkles } from 'lucide-react';

function AdditionalDetailsCard({ previewData, onChange, user, onUserUpdate }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [coApplicants, setCoApplicants] = useState([]);

  // Use a ref to track whether the update was self-triggered to prevent re-render lag
  const isSelfTriggeredRef = useRef(false);

  // Initialize and synchronize state when user changes
  useEffect(() => {
    if (isSelfTriggeredRef.current) {
      isSelfTriggeredRef.current = false;
      return;
    }

    setName(user?.name || '');
    setEmail(user?.email || '');
    setCoApplicants(user?.additionalMembers || []);
  }, [user]);

  const syncChanges = (updatedName, updatedEmail, updatedMembers) => {
    const updatedUser = {
      ...user,
      name: updatedName,
      email: updatedEmail,
      extraPersonsCount: updatedMembers.length,
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
        inventors: updatedMembers.map(m => ({
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
        inventors: updatedMembers.map(m => ({
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
  };

  const handleRemoveMember = (index) => {
    isSelfTriggeredRef.current = true;
    const newCoApplicants = coApplicants.filter((_, idx) => idx !== index);
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
    <div className="card additional-details-card" style={{ padding: '24px', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E5E7EB' }}>
      <div className="card-header" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '18px', borderBottom: '1px solid #F3F4F6', paddingBottom: '12px' }}>
        <Sparkles className="card-header-icon" style={{ color: '#0052cc' }} size={20} />
        <span className="card-header-title" style={{ fontWeight: '600', fontSize: '1.1rem', color: '#1F2937' }}>Applicant Details</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {/* College Name */}
        <div className="form-group">
          <label className="form-label" htmlFor="details-name" style={{ color: '#4B5563', fontWeight: '500' }}>
            College Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} />
            <input
              id="details-name"
              type="text"
              className="login-input"
              style={{
                background: '#F9FAFB',
                border: '1px solid #D1D5DB',
                color: '#1F2937',
                paddingLeft: '42px',
                paddingRight: '16px'
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
          <label className="form-label" htmlFor="details-email" style={{ color: '#4B5563', fontWeight: '500' }}>
            Principal Name *
          </label>
          <div className="input-container">
            <User className="input-icon" style={{ color: '#9CA3AF' }} />
            <input
              id="details-email"
              type="text"
              className="login-input"
              style={{
                background: '#F9FAFB',
                border: '1px solid #D1D5DB',
                color: '#1F2937',
                paddingLeft: '42px',
                paddingRight: '16px'
              }}
              placeholder="Enter principal's name"
              value={email}
              onChange={(e) => handleEmailChange(e.target.value)}
              required
            />
          </div>
        </div>

        {/* Additional Members Section Header */}
        <div style={{
          marginTop: '8px',
          borderTop: '1px solid #F3F4F6',
          paddingTop: '16px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <span style={{ fontSize: '13px', fontWeight: '600', color: '#1F2937', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Users size={16} style={{ color: '#0052cc' }} /> Inventors ({coApplicants.length}/8)
          </span>
          {coApplicants.length < 8 && (
            <button
              type="button"
              onClick={handleAddMember}
              style={{
                background: '#0052cc',
                color: '#FFF',
                border: 'none',
                padding: '6px 12px',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                transition: 'background 0.2s'
              }}
              onMouseEnter={(e) => e.target.style.background = '#0041a8'}
              onMouseLeave={(e) => e.target.style.background = '#0052cc'}
            >
              + Add Inventor
            </button>
          )}
        </div>

        {/* List of Additional Members */}
        {coApplicants.length > 0 ? (
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '16px',
            backgroundColor: '#F9FAFB',
            padding: '16px',
            borderRadius: '8px',
            border: '1px dashed #E5E7EB'
          }}>
            {coApplicants.map((member, index) => (
              <div key={index} style={{
                display: 'flex',
                flexDirection: 'column',
                gap: '10px',
                paddingBottom: index === coApplicants.length - 1 ? '0' : '16px',
                borderBottom: index === coApplicants.length - 1 ? 'none' : '1px solid #E5E7EB'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '11px', fontWeight: '700', color: '#4B5563', textTransform: 'uppercase' }}>
                    Inventor #{index + 1}
                  </span>
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
                      display: 'flex',
                      alignItems: 'center',
                      gap: '2px',
                      padding: '2px 6px',
                      borderRadius: '4px',
                      transition: 'background 0.2s'
                    }}
                    onMouseEnter={(e) => e.target.style.background = '#FEE2E2'}
                    onMouseLeave={(e) => e.target.style.background = 'transparent'}
                  >
                    Remove
                  </button>
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor={`member-name-${index}`} style={{ fontSize: '11px', color: '#6B7280', fontWeight: '500' }}>
                    Name *
                  </label>
                  <input
                    id={`member-name-${index}`}
                    type="text"
                    className="login-input"
                    style={{
                      background: '#FFF',
                      border: '1px solid #D1D5DB',
                      color: '#1F2937',
                      paddingLeft: '12px',
                      paddingRight: '12px',
                      height: '38px',
                      fontSize: '13px'
                    }}
                    placeholder={`Enter inventor #${index + 1} name`}
                    value={member.name}
                    onChange={(e) => handleMemberChange(index, e.target.value)}
                    required
                  />
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div style={{
            textAlign: 'center',
            padding: '20px',
            color: '#6B7280',
            fontSize: '13px',
            backgroundColor: '#F9FAFB',
            borderRadius: '8px',
            border: '1px dashed #E5E7EB'
          }}>
            No inventors added yet.
          </div>
        )}
      </div>
    </div>
  );
}

export default AdditionalDetailsCard;
