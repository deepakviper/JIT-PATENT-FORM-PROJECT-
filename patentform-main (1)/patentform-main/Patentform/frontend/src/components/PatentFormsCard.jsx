import React from 'react';
import { FileText, ListChecks } from 'lucide-react';

const formPages = [
  {
    id: 1,
    title: 'Form 1',
    subtitle: 'Application for Grant of Patent',
    iconClass: 'form-icon-blue',
  },
  {
    id: 2,
    title: 'Form 2',
    subtitle: 'Complete Specification',
    iconClass: 'form-icon-green',
  },
  {
    id: 3,
    title: 'Form 3',
    subtitle: 'Statement & Undertaking',
    iconClass: 'form-icon-orange',
  },
  {
    id: 5,
    title: 'Form 5',
    subtitle: 'Declaration as to Inventorship',
    iconClass: 'form-icon-purple',
  },
  {
    id: 9,
    title: 'Form 9',
    subtitle: 'Request for Publication',
    iconClass: 'form-icon-blue',
  },
  {
    id: 28,
    title: 'Form 28',
    subtitle: 'To Claim Status of Small Entity / Startup',
    iconClass: 'form-icon-green',
  },
];

function PatentFormsCard({ selectedForms = [], setSelectedForms }) {
  // Toggle individual form
  const handleCheck = (id) => {
    const formKey = `form${id}`;

    if (selectedForms.includes(formKey)) {
      setSelectedForms(selectedForms.filter((item) => item !== formKey));
    } else {
      setSelectedForms([...selectedForms, formKey]);
    }
  };

  // Toggle all forms
  const handleCompleteAll = () => {
    const allFormKeys = formPages.map((page) => `form${page.id}`);

    const isAllChecked = formPages.every((page) =>
      selectedForms.includes(`form${page.id}`)
    );

    if (isAllChecked) {
      setSelectedForms([]);
    } else {
      setSelectedForms(allFormKeys);
    }
  };

  // Check if all forms are selected
  const isAllChecked =
    formPages.length > 0 &&
    formPages.every((page) =>
      selectedForms.includes(`form${page.id}`)
    );

  return (
    <div className="card patent-forms-card">
      <div className="card-header">
        <FileText className="card-header-icon" />
        <span className="card-header-title">Select Patent Forms</span>
      </div>

      <div className="form-list">
        {formPages.map((page) => (
          <div
            key={page.id}
            className="form-item"
            onClick={() => handleCheck(page.id)}
            style={{ cursor: 'pointer' }}
          >
            <input
              type="checkbox"
              className="form-checkbox"
              checked={selectedForms.includes(`form${page.id}`)}
              onChange={(e) => {
                e.stopPropagation();
                handleCheck(page.id);
              }}
            />

            <div className={`form-icon-container ${page.iconClass}`}>
              <FileText size={18} />
            </div>

            <div className="form-text">
              <div className="form-page-title">{page.title}</div>
              <div className="form-page-subtitle">{page.subtitle}</div>
            </div>
          </div>
        ))}

        {/* All Forms */}
        <div
          className="form-item"
          onClick={handleCompleteAll}
          style={{ cursor: 'pointer' }}
        >
          <input
            type="checkbox"
            className="form-checkbox"
            checked={isAllChecked}
            onChange={(e) => {
              e.stopPropagation();
              handleCompleteAll();
            }}
          />

          <div className="form-icon-container form-icon-purple">
            <ListChecks size={18} />
          </div>

          <div className="form-text">
            <div className="form-page-title">All Forms</div>
            <div className="form-page-subtitle">
              Select or Deselect All Patent Forms
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default PatentFormsCard;